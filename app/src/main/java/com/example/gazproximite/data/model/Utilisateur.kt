package com.example.gazproximite.data.model

import com.google.firebase.firestore.DocumentId

// Profil enregistré dans la collection Firestore "utilisateurs".
data class Utilisateur(
    @DocumentId
    val uid: String = "",

    val nom: String = "",
    val prenom: String = "",
    val telephone: String = "",
    val adresse: String = "",
    val email: String = "",
    val role: String = "",
    val depotId: String = ""
)
