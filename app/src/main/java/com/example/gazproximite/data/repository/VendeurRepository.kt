package com.example.gazproximite.data.repository

import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.model.Vente
import com.example.gazproximite.data.model.cleStock
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Repository de l'espace vendeur :
// connexion par e-mail, gestion du stock, des prix, des ventes et du dépôt.
class VendeurRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val depotRepository = DepotRepository()

    private fun refDepot(depotId: String) =
        db.collection("depots").document(depotId)

    private fun refVentes(depotId: String) =
        refDepot(depotId).collection("ventes")


    // =========================================================
    // 1. CONNEXION / INSCRIPTION / DÉCONNEXION
    // =========================================================

    // Connecte un vendeur avec son e-mail et son mot de passe.
    // Le compte doit avoir le rôle "vendeur" dans Firestore.
    suspend fun connecter(email: String, motDePasse: String): Result<Utilisateur> {
        return try {
            val resultat = auth.signInWithEmailAndPassword(email.trim(), motDePasse).await()
            val uid = resultat.user?.uid
                ?: return Result.failure(Exception("Utilisateur introuvable."))

            val vendeur = lireProfil(uid)
            if (vendeur == null || vendeur.role != "vendeur" || vendeur.depotId.isBlank()) {
                auth.signOut()
                Result.failure(Exception("Ce compte n'est pas un compte vendeur."))
            } else {
                Result.success(vendeur)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Crée le compte du vendeur, son dépôt et son profil Firestore.
    suspend fun inscrire(
        email: String,
        motDePasse: String,
        nom: String,
        prenom: String,
        nomDepot: String,
        commune: String,
        telephone: String,
        latitude: Double,
        longitude: Double
    ): Result<Utilisateur> {
        return try {
            // Étape 1 : compte Firebase Authentication.
            // Si une inscription précédente s'est arrêtée après la création du compte
            // (ex. refus de Firestore), l'e-mail existe déjà : on se connecte à ce compte
            // pour terminer l'inscription.
            val resultat = try {
                auth.createUserWithEmailAndPassword(email.trim(), motDePasse).await()
            } catch (e: FirebaseAuthUserCollisionException) {
                auth.signInWithEmailAndPassword(email.trim(), motDePasse).await()
            }
            val uid = resultat.user?.uid
                ?: return Result.failure(Exception("Utilisateur introuvable."))

            // Étape 2 : le profil vendeur, AVANT le dépôt.
            // Les règles Firestore n'autorisent la création d'un dépôt que si le
            // profil "vendeur" existe déjà. Elles interdisent aussi de modifier un
            // profil : s'il existe déjà (inscription interrompue), on le réutilise.
            val profilExistant = lireProfil(uid)

            val vendeur = if (profilExistant != null) {
                if (profilExistant.role != "vendeur" || profilExistant.depotId.isBlank()) {
                    return Result.failure(
                        Exception("Ce compte existe déjà mais n'est pas un compte vendeur.")
                    )
                }
                profilExistant
            } else {
                // Identifiant du futur dépôt, réservé dès maintenant.
                val depotId = db.collection("depots").document().id
                val nouveauProfil = Utilisateur(
                    uid = uid,
                    nom = nom.trim(),
                    prenom = prenom.trim(),
                    telephone = telephone.trim(),
                    email = email.trim(),
                    role = "vendeur",
                    depotId = depotId
                )
                db.collection("utilisateurs").document(uid).set(
                    hashMapOf(
                        "nom" to nouveauProfil.nom,
                        "prenom" to nouveauProfil.prenom,
                        "telephone" to nouveauProfil.telephone,
                        "email" to nouveauProfil.email,
                        "role" to "vendeur",
                        "depotId" to depotId
                    )
                ).await()
                nouveauProfil
            }

            // Étape 3 : le dépôt, s'il n'a pas encore été créé.
            val depotExiste = refDepot(vendeur.depotId).get().await().exists()
            if (!depotExiste) {
                depotRepository.creerDepot(
                    depotId = vendeur.depotId,
                    nom = nomDepot.trim(),
                    commune = commune.trim(),
                    telephone = telephone.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    proprietaireUid = uid
                )
            }

            Result.success(vendeur)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Profil du vendeur déjà connecté (null si personne ou si ce n'est pas un vendeur).
    suspend fun vendeurConnecte(): Utilisateur? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            lireProfil(uid)?.takeIf { it.role == "vendeur" && it.depotId.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun deconnecter() {
        auth.signOut()
    }

    private suspend fun lireProfil(uid: String): Utilisateur? {
        val document = db.collection("utilisateurs").document(uid).get().await()
        return if (document.exists()) document.toObject(Utilisateur::class.java) else null
    }


    // =========================================================
    // 2. SUIVI EN DIRECT DU DÉPÔT ET DES VENTES
    // =========================================================

    // Envoie le dépôt à chaque modification dans Firestore.
    fun observerDepot(depotId: String): Flow<Result<Depot?>> = callbackFlow {
        val ecoute = refDepot(depotId).addSnapshotListener { document, erreur ->
            if (erreur != null) {
                trySend(Result.failure(erreur))
            } else {
                trySend(Result.success(document?.toObject(Depot::class.java)))
            }
        }
        awaitClose { ecoute.remove() }
    }

    // Envoie les 200 dernières ventes, de la plus récente à la plus ancienne.
    fun observerVentes(depotId: String): Flow<Result<List<Vente>>> = callbackFlow {
        val ecoute = refVentes(depotId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { resultat, erreur ->
                if (erreur != null) {
                    trySend(Result.failure(erreur))
                } else {
                    trySend(Result.success(resultat?.toObjects(Vente::class.java).orEmpty()))
                }
            }
        awaitClose { ecoute.remove() }
    }


    // =========================================================
    // 3. STOCK ET PRIX
    // =========================================================

    // Fixe le nombre de bouteilles d'une marque et d'un format.
    // La disponibilité vue par les clients ("stock") suit automatiquement.
    suspend fun modifierQuantite(
        depotId: String,
        marque: Marque,
        format: Format,
        quantite: Long
    ): Result<Unit> = executer {
        val cle = cleStock(marque, format)
        val nouvelleQuantite = quantite.coerceAtLeast(0)
        refDepot(depotId).update(
            mapOf(
                "quantites.$cle" to nouvelleQuantite,
                "stock.$cle" to (nouvelleQuantite > 0),
                "misAJourLe" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // Fixe le prix de vente d'une bouteille, en F CFA.
    suspend fun modifierPrix(
        depotId: String,
        marque: Marque,
        format: Format,
        prix: Long
    ): Result<Unit> = executer {
        refDepot(depotId).update(
            mapOf(
                "prix.${cleStock(marque, format)}" to prix.coerceAtLeast(0),
                "misAJourLe" to FieldValue.serverTimestamp()
            )
        ).await()
    }


    // =========================================================
    // 4. VENTES
    // =========================================================

    // Enregistre une vente et retire les bouteilles du stock, en une seule
    // opération (transaction) : impossible de vendre plus que le stock.
    suspend fun enregistrerVente(
        depotId: String,
        marque: Marque,
        format: Format,
        quantite: Long,
        prixUnitaire: Long
    ): Result<Unit> = executer {
        require(quantite > 0) { "La quantité doit être supérieure à 0." }

        val cle = cleStock(marque, format)
        val vendeurUid = auth.currentUser?.uid.orEmpty()

        db.runTransaction { transaction ->
            val document = transaction.get(refDepot(depotId))
            val enStock = (document.get("quantites.$cle") as? Number)?.toLong() ?: 0L

            if (enStock < quantite) {
                throw IllegalStateException(
                    "Stock insuffisant : il reste $enStock bouteille(s) ${format.name} ${marque.libelle}."
                )
            }

            val reste = enStock - quantite
            transaction.update(
                refDepot(depotId),
                mapOf(
                    "quantites.$cle" to reste,
                    "stock.$cle" to (reste > 0),
                    "misAJourLe" to FieldValue.serverTimestamp()
                )
            )

            transaction.set(
                refVentes(depotId).document(),
                hashMapOf(
                    "marque" to marque.name.lowercase(),
                    "format" to format.name.lowercase(),
                    "quantite" to quantite,
                    "prixUnitaire" to prixUnitaire,
                    "total" to quantite * prixUnitaire,
                    "date" to FieldValue.serverTimestamp(),
                    "vendeurUid" to vendeurUid
                )
            )
        }.await()
    }

    // Annule une vente : les bouteilles reviennent dans le stock.
    suspend fun annulerVente(depotId: String, vente: Vente): Result<Unit> = executer {
        val marque = vente.marqueEnum ?: error("Marque inconnue.")
        val format = vente.formatEnum ?: error("Format inconnu.")
        val cle = cleStock(marque, format)

        db.runTransaction { transaction ->
            val document = transaction.get(refDepot(depotId))
            val enStock = (document.get("quantites.$cle") as? Number)?.toLong() ?: 0L
            val total = enStock + vente.quantite

            transaction.update(
                refDepot(depotId),
                mapOf(
                    "quantites.$cle" to total,
                    "stock.$cle" to (total > 0),
                    "misAJourLe" to FieldValue.serverTimestamp()
                )
            )
            transaction.delete(refVentes(depotId).document(vente.id))
        }.await()
    }


    // =========================================================
    // 5. INFORMATIONS DU DÉPÔT
    // =========================================================

    suspend fun modifierOuverture(depotId: String, estOuvert: Boolean): Result<Unit> = executer {
        refDepot(depotId).update(
            mapOf(
                "estOuvert" to estOuvert,
                "misAJourLe" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun modifierInformations(
        depotId: String,
        nom: String,
        commune: String,
        telephone: String,
        heureOuverture: String,
        heureFermeture: String,
        photoUrl: String
    ): Result<Unit> = executer {
        refDepot(depotId).update(
            mapOf(
                "nom" to nom.trim(),
                "commune" to commune.trim(),
                "telephone" to telephone.trim(),
                "heureOuverture" to heureOuverture.trim(),
                "heureFermeture" to heureFermeture.trim(),
                "photoUrl" to photoUrl.trim(),
                "misAJourLe" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // Exécute une opération Firebase et transforme une erreur en Result.failure.
    private suspend fun executer(operation: suspend () -> Unit): Result<Unit> {
        return try {
            operation()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
