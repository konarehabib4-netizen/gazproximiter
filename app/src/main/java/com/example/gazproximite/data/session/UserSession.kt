package com.example.gazproximite.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.gazproximite.data.model.Utilisateur
import com.google.firebase.auth.FirebaseAuth

/**
 * Modèle de données représentant le profil de l'utilisateur connecté.
 */
data class UserProfile(
    val email: String = "",
    val nom: String = "",
    val prenom: String = "",
    val contact: String = "",
    val ville: String = "Abidjan",
    val commune: String = "",
    val isLoggedIn: Boolean = false,
)

/**
 * [UserSessionManager] - Copie locale du profil du client connecté.
 *
 * La vraie connexion est gérée par Firebase Authentication (SMS) et le profil
 * est enregistré dans Firestore. Ce gestionnaire garde une copie sur le téléphone
 * pour afficher le profil immédiatement, ainsi que les préférences locales
 * (favoris, mode démonstration).
 */
object UserSessionManager {
    private const val PREF_NAME = "yagaz_user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_NOM = "user_nom"
    private const val KEY_PRENOM = "user_prenom"
    private const val KEY_CONTACT = "user_contact"
    private const val KEY_VILLE = "user_ville"
    private const val KEY_COMMUNE = "user_commune"
    private const val KEY_FAVORIS = "favoris"
    private const val KEY_MODE_DEMO = "mode_demo"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Enregistre la copie locale du profil Firestore après la connexion.
     */
    fun saveUserSession(context: Context, utilisateur: Utilisateur) {
        getPrefs(context).edit {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_EMAIL, utilisateur.email)
            putString(KEY_NOM, utilisateur.nom)
            putString(KEY_PRENOM, utilisateur.prenom)
            putString(KEY_CONTACT, utilisateur.telephone)
            putString(KEY_COMMUNE, utilisateur.adresse)
        }
    }

    /**
     * Récupère les informations du profil utilisateur enregistré.
     */
    fun getUserProfile(context: Context): UserProfile {
        val prefs = getPrefs(context)
        if (!isLoggedIn(context)) return UserProfile()

        return UserProfile(
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            nom = prefs.getString(KEY_NOM, "") ?: "",
            prenom = prefs.getString(KEY_PRENOM, "") ?: "",
            contact = prefs.getString(KEY_CONTACT, "") ?: "",
            ville = prefs.getString(KEY_VILLE, "Abidjan") ?: "Abidjan",
            commune = prefs.getString(KEY_COMMUNE, "") ?: "",
            isLoggedIn = true,
        )
    }

    /**
     * Vérifie si le client est connecté : il faut une session Firebase active
     * ET un profil enregistré sur le téléphone.
     */
    fun isLoggedIn(context: Context): Boolean {
        return FirebaseAuth.getInstance().currentUser != null &&
                getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Déconnecte le client de Firebase et efface la copie locale du profil.
     * Les favoris et le mode démonstration sont conservés.
     */
    fun logout(context: Context) {
        FirebaseAuth.getInstance().signOut()
        getPrefs(context).edit {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_EMAIL)
            remove(KEY_NOM)
            remove(KEY_PRENOM)
            remove(KEY_CONTACT)
            remove(KEY_COMMUNE)
        }
    }

    // =========================================================
    // FAVORIS (identifiants des dépôts, gardés sur le téléphone)
    // =========================================================

    fun getFavoris(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_FAVORIS, emptySet())?.toSet() ?: emptySet()
    }

    fun basculerFavori(context: Context, depotId: String): Set<String> {
        val favoris = getFavoris(context)
        val nouveaux = if (depotId in favoris) favoris - depotId else favoris + depotId
        getPrefs(context).edit { putStringSet(KEY_FAVORIS, nouveaux) }
        return nouveaux
    }

    // =========================================================
    // MODE DÉMONSTRATION (dépôts fictifs pour la présentation)
    // =========================================================

    fun isModeDemo(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MODE_DEMO, false)
    }

    fun setModeDemo(context: Context, actif: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_MODE_DEMO, actif) }
    }
}
