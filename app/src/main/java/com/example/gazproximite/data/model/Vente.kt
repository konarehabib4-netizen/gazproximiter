package com.example.gazproximite.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

// Vente enregistrée par un vendeur, dans depots/{depotId}/ventes/{id}.
data class Vente(
    @DocumentId
    val id: String = "",

    // Clé de la marque et du format, en minuscules (ex. "petroci", "b12").
    val marque: String = "",
    val format: String = "",

    val quantite: Long = 0,

    // Prix d'une bouteille et montant total, en F CFA.
    val prixUnitaire: Long = 0,
    val total: Long = 0,

    // Date fixée par le serveur Firebase (null tant que la vente n'est pas synchronisée).
    val date: Timestamp? = null,

    val vendeurUid: String = ""
) {
    // Marque et format sous forme d'enum (null si la clé est inconnue).
    @get:Exclude
    val marqueEnum: Marque?
        get() = Marque.entries.firstOrNull { it.name.equals(marque, ignoreCase = true) }

    @get:Exclude
    val formatEnum: Format?
        get() = Format.entries.firstOrNull { it.name.equals(format, ignoreCase = true) }
}
