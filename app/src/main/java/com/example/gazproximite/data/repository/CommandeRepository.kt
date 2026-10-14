package com.example.gazproximite.data.repository

import com.example.gazproximite.data.model.Commande
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.LigneCommande
import com.example.gazproximite.data.model.ModeRemise
import com.example.gazproximite.data.model.StatutCommande
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.model.cleStock
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Repository des commandes, partagé par le client et le vendeur.
//
// Collection Firestore "commandes" :
// - le client crée la commande et peut l'annuler tant qu'elle est en attente ;
// - le vendeur l'accepte, la refuse, la prépare puis la termine ;
// - terminer une commande retire les bouteilles du stock et crée les ventes.
class CommandeRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val commandes = db.collection("commandes")

    private fun refDepot(depotId: String) = db.collection("depots").document(depotId)


    // =========================================================
    // 1. CÔTÉ CLIENT
    // =========================================================

    // Envoie une nouvelle commande au dépôt. Renvoie l'identifiant de la commande.
    suspend fun passerCommande(
        depot: Depot,
        client: Utilisateur,
        lignes: List<LigneCommande>,
        mode: ModeRemise,
        adresseLivraison: String,
        note: String
    ): Result<String> {
        return try {
            val clientUid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Connecte-toi pour commander."))

            val lignesValides = lignes.filter { it.quantite > 0 }
            require(lignesValides.isNotEmpty()) { "Choisis au moins une bouteille." }
            require(depot.proprietaireUid.isNotBlank()) {
                "Ce dépôt ne prend pas encore les commandes en ligne."
            }
            require(mode != ModeRemise.LIVRAISON || adresseLivraison.isNotBlank()) {
                "Indique l'adresse de livraison."
            }

            val reference = commandes.document()
            reference.set(
                hashMapOf(
                    "clientUid" to clientUid,
                    "clientNom" to "${client.prenom} ${client.nom}".trim(),
                    "clientTelephone" to client.telephone,
                    "depotId" to depot.id,
                    "depotNom" to depot.nom,
                    "depotTelephone" to depot.telephone,
                    "proprietaireUid" to depot.proprietaireUid,
                    "lignes" to lignesValides.map { ligne ->
                        hashMapOf(
                            "marque" to ligne.marque,
                            "format" to ligne.format,
                            "quantite" to ligne.quantite,
                            "prixUnitaire" to ligne.prixUnitaire
                        )
                    },
                    "total" to lignesValides.sumOf { it.total },
                    "mode" to mode.cle,
                    "adresseLivraison" to adresseLivraison.trim(),
                    "note" to note.trim(),
                    "statut" to StatutCommande.EN_ATTENTE.cle,
                    "motifRefus" to "",
                    "creeLe" to FieldValue.serverTimestamp(),
                    "misAJourLe" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(reference.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Commandes du client connecté, de la plus récente à la plus ancienne.
    fun observerCommandesClient(clientUid: String): Flow<Result<List<Commande>>> =
        observer(commandes.whereEqualTo("clientUid", clientUid))

    // Le client annule sa commande, seulement si le vendeur ne l'a pas encore acceptée.
    suspend fun annulerParClient(commandeId: String): Result<Unit> = executer {
        db.runTransaction { transaction ->
            val reference = commandes.document(commandeId)
            val statut = transaction.get(reference).getString("statut")

            check(statut == StatutCommande.EN_ATTENTE.cle) {
                "Le dépôt a déjà pris en charge cette commande : appelle-le pour l'annuler."
            }

            transaction.update(
                reference,
                mapOf(
                    "statut" to StatutCommande.ANNULEE.cle,
                    "misAJourLe" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }


    // =========================================================
    // 2. CÔTÉ VENDEUR
    // =========================================================

    // Commandes reçues par le vendeur connecté (tous statuts).
    fun observerCommandesVendeur(vendeurUid: String): Flow<Result<List<Commande>>> =
        observer(commandes.whereEqualTo("proprietaireUid", vendeurUid))

    suspend fun accepter(commandeId: String): Result<Unit> =
        changerStatut(commandeId, StatutCommande.ACCEPTEE, depuis = setOf(StatutCommande.EN_ATTENTE))

    suspend fun marquerPrete(commandeId: String): Result<Unit> =
        changerStatut(commandeId, StatutCommande.PRETE, depuis = setOf(StatutCommande.ACCEPTEE))

    suspend fun refuser(commandeId: String, motif: String): Result<Unit> =
        changerStatut(
            commandeId,
            StatutCommande.REFUSEE,
            depuis = setOf(StatutCommande.EN_ATTENTE, StatutCommande.ACCEPTEE, StatutCommande.PRETE),
            champsEnPlus = mapOf("motifRefus" to motif.trim())
        )

    // Termine la commande (livrée ou retirée) en une seule transaction :
    // vérifie le stock, retire les bouteilles, crée une vente par ligne
    // et passe la commande au statut "terminée".
    suspend fun terminer(commandeId: String): Result<Unit> = executer {
        val vendeurUid = auth.currentUser?.uid.orEmpty()

        db.runTransaction { transaction ->
            val refCommande = commandes.document(commandeId)
            val commande = transaction.get(refCommande).toObject(Commande::class.java)
                ?: error("Commande introuvable.")

            check(commande.statutEnum == StatutCommande.ACCEPTEE || commande.statutEnum == StatutCommande.PRETE) {
                "Cette commande ne peut plus être terminée (${commande.statutEnum.libelle})."
            }

            val refDepot = refDepot(commande.depotId)
            val depot = transaction.get(refDepot)

            // Toutes les lectures d'abord (règle des transactions Firestore),
            // puis vérification du stock de chaque ligne.
            val restes = mutableMapOf<String, Long>()
            commande.lignes.forEach { ligne ->
                val marque = ligne.marqueEnum ?: error("Marque inconnue : ${ligne.marque}")
                val format = ligne.formatEnum ?: error("Format inconnu : ${ligne.format}")
                val cle = cleStock(marque, format)

                val disponible = restes[cle]
                    ?: (depot.get("quantites.$cle") as? Number)?.toLong()
                    ?: 0L

                check(disponible >= ligne.quantite) {
                    "Stock insuffisant pour ${format.name} ${marque.libelle} : " +
                            "il reste $disponible bouteille(s), la commande en demande ${ligne.quantite}."
                }
                restes[cle] = disponible - ligne.quantite
            }

            // Mise à jour du stock.
            val miseAJourStock = mutableMapOf<String, Any>("misAJourLe" to FieldValue.serverTimestamp())
            restes.forEach { (cle, reste) ->
                miseAJourStock["quantites.$cle"] = reste
                miseAJourStock["stock.$cle"] = reste > 0
            }
            transaction.update(refDepot, miseAJourStock)

            // Une vente par ligne de commande.
            commande.lignes.forEach { ligne ->
                transaction.set(
                    refDepot.collection("ventes").document(),
                    hashMapOf(
                        "marque" to ligne.marque,
                        "format" to ligne.format,
                        "quantite" to ligne.quantite,
                        "prixUnitaire" to ligne.prixUnitaire,
                        "total" to ligne.total,
                        "date" to FieldValue.serverTimestamp(),
                        "vendeurUid" to vendeurUid,
                        "commandeId" to commandeId
                    )
                )
            }

            transaction.update(
                refCommande,
                mapOf(
                    "statut" to StatutCommande.TERMINEE.cle,
                    "misAJourLe" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }


    // =========================================================
    // 3. OUTILS
    // =========================================================

    // Change le statut si la commande est dans l'un des statuts attendus.
    private suspend fun changerStatut(
        commandeId: String,
        nouveau: StatutCommande,
        depuis: Set<StatutCommande>,
        champsEnPlus: Map<String, Any> = emptyMap()
    ): Result<Unit> = executer {
        db.runTransaction { transaction ->
            val reference = commandes.document(commandeId)
            val actuel = StatutCommande.depuisCle(transaction.get(reference).getString("statut").orEmpty())

            check(actuel in depuis) {
                "Action impossible : la commande est « ${actuel.libelle} »."
            }

            transaction.update(
                reference,
                champsEnPlus + mapOf(
                    "statut" to nouveau.cle,
                    "misAJourLe" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    // Suivi en direct d'une requête, triée de la plus récente à la plus ancienne.
    // Le tri est fait dans l'application : aucun index composite n'est nécessaire.
    private fun observer(requete: Query): Flow<Result<List<Commande>>> = callbackFlow {
        val ecoute = requete.addSnapshotListener { resultat, erreur ->
            if (erreur != null) {
                trySend(Result.failure(erreur))
            } else {
                val liste = resultat?.toObjects(Commande::class.java).orEmpty()
                    .sortedByDescending { it.creeLe?.seconds ?: Long.MAX_VALUE }
                trySend(Result.success(liste))
            }
        }
        awaitClose { ecoute.remove() }
    }

    private suspend fun executer(operation: suspend () -> Unit): Result<Unit> {
        return try {
            operation()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
