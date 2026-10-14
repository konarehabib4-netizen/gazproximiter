package com.example.gazproximite.data.repository

import com.example.gazproximite.data.model.Utilisateur
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ClientRepository {

    // Connexion à Firestore.
    private val db = FirebaseFirestore.getInstance()

    // =====================================================
    // 1. CRÉER LE PROFIL D'UN CLIENT
    // =====================================================

    suspend fun creerProfilClient(
        uid: String,
        nom: String,
        prenom: String,
        adresse: String,
        telephone: String,
        email: String = ""
    ): Result<Unit> {

        return try {
            // Préparer les informations du client.
            val client = hashMapOf(
                "nom" to nom.trim(),
                "prenom" to prenom.trim(),
                "adresse" to adresse.trim(),
                "telephone" to telephone.trim(),
                "email" to email.trim(),
                "role" to "client"
            )

            // Enregistrer le profil dans utilisateurs/{uid}.
            db.collection("utilisateurs")
                .document(uid)
                .set(client)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =====================================================
    // 2. RÉCUPÉRER LE PROFIL D'UN CLIENT
    // =====================================================

    suspend fun recupererProfilClient(
        uid: String
    ): Result<Utilisateur?> {

        return try {
            // Rechercher le document correspondant à l'UID Firebase.
            val document = db.collection("utilisateurs")
                .document(uid)
                .get()
                .await()

            // Si le profil existe, on le transforme en objet Utilisateur.
            // Sinon, on renvoie null.
            if (document.exists()) {
                Result.success(document.toObject(Utilisateur::class.java))
            } else {
                Result.success(null)
            }

        } catch (e: Exception) {
            // Renvoyer l'erreur en cas de problème Firestore.
            Result.failure(e)
        }
    }
}
