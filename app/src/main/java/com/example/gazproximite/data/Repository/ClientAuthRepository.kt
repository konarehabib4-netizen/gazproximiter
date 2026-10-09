
package com.example.gazproximite.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit


class ClientAuthRepository {

    // =========================================================
    // FIREBASE AUTHENTICATION
    // =========================================================

    private val auth =
        FirebaseAuth.getInstance()


    // =========================================================
    // IDENTIFIANT DE VÉRIFICATION
    // =========================================================

    // Firebase nous fournit cet identifiant lorsque
    // le SMS est envoyé.
    //
    // Nous l'utiliserons ensuite avec le code reçu
    // par le client.
    private var verificationId: String? = null


    // =========================================================
    // ENVOYER LE CODE SMS
    // =========================================================

    fun envoyerCodeSMS(
        activity: Activity,
        telephone: String,
        onCodeEnvoye: () -> Unit,
        onErreur: (String) -> Unit
    ) {

        // Configuration de Firebase Phone Authentication.
        val options =
            PhoneAuthOptions.newBuilder(auth)

                // Numéro de téléphone du client.
                //
                // Exemple :
                // +2250700000000
                .setPhoneNumber(telephone)

                // Le code reste valable pendant 60 secondes.
                .setTimeout(
                    60L,
                    TimeUnit.SECONDS
                )

                // Activité Android utilisée par Firebase.
                .setActivity(activity)

                // Callbacks permettant de recevoir
                // les réponses de Firebase.
                .setCallbacks(

                    object :
                        PhoneAuthProvider.OnVerificationStateChangedCallbacks() {


                        // =================================================
                        // SMS ENVOYÉ
                        // =================================================

                        override fun onCodeSent(
                            verificationIdFirebase: String,
                            token:
                            PhoneAuthProvider.ForceResendingToken
                        ) {

                            // On sauvegarde l'identifiant
                            // pour pouvoir vérifier le code plus tard.
                            verificationId =
                                verificationIdFirebase


                            // On informe l'interface
                            // que le SMS a été envoyé.
                            onCodeEnvoye()
                        }


                        // =================================================
                        // VÉRIFICATION AUTOMATIQUE
                        // =================================================

                        override fun onVerificationCompleted(
                            credential: PhoneAuthCredential
                        ) {

                            // Dans certains cas Firebase peut
                            // vérifier automatiquement le numéro.
                            //
                            // On connecte alors directement
                            // l'utilisateur.
                            auth
                                .signInWithCredential(
                                    credential
                                )
                                .addOnSuccessListener {

                                    onCodeEnvoye()
                                }
                                .addOnFailureListener { exception ->

                                    onErreur(
                                        exception.message
                                            ?: "Erreur de connexion"
                                    )
                                }
                        }


                        // =================================================
                        // ÉCHEC DE LA VÉRIFICATION
                        // =================================================

                        override fun onVerificationFailed(
                            exception: FirebaseException
                        ) {

                            onErreur(
                                exception.message
                                    ?: "Erreur lors de la vérification"
                            )
                        }
                    }
                )
                .build()


        // Demande à Firebase d'envoyer le SMS.
        PhoneAuthProvider.verifyPhoneNumber(
            options
        )
    }


    // =========================================================
    // VÉRIFIER LE CODE SMS
    // =========================================================

    suspend fun verifierCodeSMS(
        code: String
    ): Result<String> {

        return try {

            // On vérifie que Firebase nous a bien fourni
            // un identifiant de vérification.
            val idVerification =
                verificationId
                    ?: return Result.failure(
                        Exception(
                            "Aucune vérification en cours"
                        )
                    )


            // Création du credential Firebase.
            val credential =
                PhoneAuthProvider.getCredential(
                    idVerification,
                    code
                )


            // Connexion avec le code reçu par SMS.
            val resultat =
                auth
                    .signInWithCredential(
                        credential
                    )
                    .await()


            // Récupération de l'utilisateur Firebase.
            val utilisateur =
                resultat.user
                    ?: return Result.failure(
                        Exception(
                            "Utilisateur introuvable"
                        )
                    )


            // Retourne l'UID du client.
            Result.success(
                utilisateur.uid
            )


        } catch (e: Exception) {

            Result.failure(e)
        }
    }


    // =========================================================
    // UTILISATEUR CONNECTÉ
    // =========================================================

    fun utilisateurConnecte() =
        auth.currentUser



    // Cette fonction demande à Firebase d'envoyer un code SMS
// au numéro de téléphone fourni pour créer un compte client.
    fun envoyerCodeSMSPourInscription(
        activity: Activity,
        telephone: String,
        onCodeEnvoye: () -> Unit,
        onConnexionAutomatique: (String) -> Unit,
        onErreur: (String) -> Unit
    ) {
        // Préparation de la demande de vérification du numéro.
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(telephone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    // Firebase a envoyé le code SMS.
                    override fun onCodeSent(
                        verificationIdFirebase: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        // On conserve l'identifiant pour vérifier le code plus tard.
                        verificationId = verificationIdFirebase

                        // On informe l'écran que le code a été envoyé.
                        onCodeEnvoye()
                    }

                    // Firebase a vérifié automatiquement le numéro.
                    override fun onVerificationCompleted(
                        credential: PhoneAuthCredential
                    ) {
                        auth.signInWithCredential(credential)
                            .addOnSuccessListener { resultat ->
                                val uid = resultat.user?.uid

                                if (uid != null) {
                                    // Le compte Firebase est connecté.
                                    onConnexionAutomatique(uid)
                                } else {
                                    onErreur("Identifiant utilisateur introuvable.")
                                }
                            }
                            .addOnFailureListener { exception ->
                                onErreur(
                                    exception.message
                                        ?: "Échec de la connexion automatique."
                                )
                            }
                    }

                    // Firebase n'a pas pu vérifier le numéro.
                    override fun onVerificationFailed(
                        exception: FirebaseException
                    ) {
                        onErreur(
                            exception.message
                                ?: "Échec de l'envoi du code SMS."
                        )
                    }
                }
            )
            .build()

        // Lancement de la vérification du numéro par Firebase.
        PhoneAuthProvider.verifyPhoneNumber(options)
    }



    // =========================================================
    // DÉCONNEXION
    // =========================================================

    fun deconnecter() {

        auth.signOut()
    }
}
