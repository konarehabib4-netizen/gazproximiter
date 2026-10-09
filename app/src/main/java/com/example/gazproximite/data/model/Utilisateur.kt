package com.example.gazproximite.data.model

import com.google.firebase.firestore.DocumentId

data class Utilisateur(
    @DocumentId val uid: String = "",
    val email: String = "",
    val role: String = "",
    val depotId: String = ""
)