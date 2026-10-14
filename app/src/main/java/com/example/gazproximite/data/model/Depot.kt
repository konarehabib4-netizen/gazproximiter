package com.example.gazproximite.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint

data class Depot(
    @DocumentId var id: String = "",
    var nom: String = "",
    var commune: String = "",
    var telephone: String = "",
    var position: GeoPoint? = null,
    var estOuvert: Boolean = false,
    var heureOuverture: String = "",
    var heureFermeture: String = "",
    var proprietaireUid: String = "",
    var stock: Map<String, Boolean> = emptyMap(),
    var misAJourLe: Timestamp? = null,

    // Lien (https://...) vers une photo du dépôt. Facultatif :
    // sans photo, l'application affiche une illustration.
    var photoUrl: String = "",

    // Vrai pour les dépôts fictifs du mode simulation de la carte.
    // Jamais enregistré dans Firestore.
    @get:Exclude
    var estSimulation: Boolean = false
)
