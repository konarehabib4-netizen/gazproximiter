package com.example.gazproximite.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

// Étapes d'une commande, du client jusqu'à la vente.
enum class StatutCommande(val cle: String, val libelle: String) {
    EN_ATTENTE("en_attente", "En attente"),
    ACCEPTEE("acceptee", "Acceptée"),
    PRETE("prete", "Prête"),
    TERMINEE("terminee", "Terminée"),
    REFUSEE("refusee", "Refusée"),
    ANNULEE("annulee", "Annulée");

    // Une commande finie ne bouge plus.
    val estFinale: Boolean
        get() = this == TERMINEE || this == REFUSEE || this == ANNULEE

    companion object {
        fun depuisCle(cle: String): StatutCommande =
            entries.firstOrNull { it.cle == cle } ?: EN_ATTENTE
    }
}

// Mode de remise de la commande.
enum class ModeRemise(val cle: String, val libelle: String) {
    RETRAIT("retrait", "Retrait au dépôt"),
    LIVRAISON("livraison", "Livraison");

    companion object {
        fun depuisCle(cle: String): ModeRemise =
            entries.firstOrNull { it.cle == cle } ?: RETRAIT
    }
}

// Une bouteille commandée (marque, format, quantité, prix au moment de la commande).
data class LigneCommande(
    val marque: String = "",
    val format: String = "",
    val quantite: Long = 0,
    val prixUnitaire: Long = 0
) {
    @get:Exclude
    val marqueEnum: Marque?
        get() = Marque.entries.firstOrNull { it.name.equals(marque, ignoreCase = true) }

    @get:Exclude
    val formatEnum: Format?
        get() = Format.entries.firstOrNull { it.name.equals(format, ignoreCase = true) }

    @get:Exclude
    val total: Long
        get() = quantite * prixUnitaire

    // Ex. "2 × B12 Petroci"
    @get:Exclude
    val libelle: String
        get() = "$quantite × ${formatEnum?.name ?: format.uppercase()} ${marqueEnum?.libelle ?: marque}"
}

// Commande passée par un client à un dépôt, dans la collection "commandes".
data class Commande(
    @DocumentId
    val id: String = "",

    // Client
    val clientUid: String = "",
    val clientNom: String = "",
    val clientTelephone: String = "",

    // Dépôt et son vendeur
    val depotId: String = "",
    val depotNom: String = "",
    val depotTelephone: String = "",
    val proprietaireUid: String = "",

    // Contenu
    val lignes: List<LigneCommande> = emptyList(),
    val total: Long = 0,

    // Remise
    val mode: String = ModeRemise.RETRAIT.cle,
    val adresseLivraison: String = "",
    val note: String = "",

    // Suivi
    val statut: String = StatutCommande.EN_ATTENTE.cle,
    val motifRefus: String = "",
    val creeLe: Timestamp? = null,
    val misAJourLe: Timestamp? = null
) {
    @get:Exclude
    val statutEnum: StatutCommande
        get() = StatutCommande.depuisCle(statut)

    @get:Exclude
    val modeEnum: ModeRemise
        get() = ModeRemise.depuisCle(mode)

    @get:Exclude
    val nombreBouteilles: Long
        get() = lignes.sumOf { it.quantite }
}
