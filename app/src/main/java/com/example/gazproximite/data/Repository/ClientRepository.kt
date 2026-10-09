
package com.example.gazproximite.data.repository
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
        telephone: String
    ): Result<Unit> {

        return try {
            // Préparer les informations du client.
            val client = hashMapOf(
                "nom" to nom.trim(),
                "prenom" to prenom.trim(),
                "adresse" to adresse.trim(),
                "telephone" to telephone.trim(),
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
    ): Result<Map<String, Any>?> {

        return try {
            // Rechercher le document correspondant à l'UID Firebase.
            val document = db.collection("utilisateurs")
                .document(uid)
                .get()
                .await()

            // Vérifier si le profil existe dans Firestore.
            if (document.exists()) {

                // Renvoyer les données du profil.
                Result.success(document.data)

            } else {

                // Aucun profil n'a été trouvé.
                Result.success(null)
            }

        } catch (e: Exception) {
            // Renvoyer l'erreur en cas de problème Firestore.
            Result.failure(e)
        }
    }
}
