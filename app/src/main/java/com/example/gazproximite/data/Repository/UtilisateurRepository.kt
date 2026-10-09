
package com.example.gazproximite.data.repository

import com.example.gazproximite.data.model.Utilisateur
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UtilisateurRepository {

    // Connexion à notre base Firestore
    private val db = FirebaseFirestore.getInstance()

    // Récupère le profil d'un utilisateur grâce à son UID
    suspend fun recupererUtilisateur(uid: String): Utilisateur? {

        // On va chercher le document dans :
        // utilisateurs / UID
        val document = db
            .collection("utilisateurs")
            .document(uid)
            .get()
            .await()

        // Si le document existe, on le transforme en objet Utilisateur
        return if (document.exists()) {
            document.toObject(Utilisateur::class.java)
        } else {
            null
        }
    }
}

