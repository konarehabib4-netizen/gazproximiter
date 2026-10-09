package com.example.gazproximite.data.repository

// Firebase Authentication
import com.google.firebase.auth.FirebaseAuth

// Permet d'attendre le résultat des opérations Firebase
import kotlinx.coroutines.tasks.await


// Repository responsable de l'authentification.
class AuthRepository {

    // Instance de Firebase Authentication.
    //
    // C'est elle qui va gérer :
    // - la connexion
    // - la déconnexion
    // - l'utilisateur actuellement connecté
    private val auth = FirebaseAuth.getInstance()


    // =========================================================
    // CONNEXION
    // =========================================================

    // Cette fonction permet de connecter un utilisateur
    // avec son adresse email et son mot de passe.
    suspend fun connecter(
        email: String,
        motDePasse: String
    ) {

        // Firebase vérifie l'email et le mot de passe.
        //
        // Si les informations sont correctes :
        // l'utilisateur est connecté.
        //
        // Si elles sont incorrectes :
        // Firebase renvoie une erreur.
        auth
            .signInWithEmailAndPassword(email, motDePasse)
            .await()
    }


    // =========================================================
    // UTILISATEUR CONNECTÉ
    // =========================================================

    // Cette fonction permet de récupérer
    // l'utilisateur actuellement connecté.
    fun utilisateurConnecte() = auth.currentUser


    // =========================================================
    // DÉCONNEXION
    // =========================================================

    // Cette fonction déconnecte l'utilisateur.
    fun deconnecter() {

        auth.signOut()
    }


// =========================================================
// INSCRIPTION
// =========================================================

    // Cette fonction permet de créer un nouveau compte
// avec une adresse email et un mot de passe.
//
// Firebase crée automatiquement un utilisateur
// et lui attribue un UID unique.
//
// La fonction retourne cet UID.
    suspend fun inscrire(
        email: String,
        motDePasse: String
    ): String {

        // Création du compte dans Firebase Authentication.
        val resultat =
            auth
                .createUserWithEmailAndPassword(
                    email,
                    motDePasse
                )
                .await()

        // On récupère l'UID du nouvel utilisateur.
        return resultat.user!!.uid
    }


}