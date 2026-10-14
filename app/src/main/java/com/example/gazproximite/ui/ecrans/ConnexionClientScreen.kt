package com.example.gazproximite.ui.ecrans

import android.app.Activity
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.example.gazproximite.data.repository.ClientAuthRepository
import com.example.gazproximite.data.repository.ClientRepository
import com.example.gazproximite.ui.composants.BandeauMessage
import com.example.gazproximite.ui.composants.ChampTexte
import kotlinx.coroutines.launch

// @Composable indique que cette fonction construit une interface Compose.
// Les paramètres permettent de communiquer avec Firebase et avec MainActivity.
@Composable
fun ConnexionClientScreen(
    activity: Activity,
    authRepository: ClientAuthRepository,
    clientRepository: ClientRepository,
    onRetourAccueil: () -> Unit,
    onConnexionReussie: (String) -> Unit
) {
    // remember conserve la valeur pendant les recompositions de l'interface.
    // mutableStateOf permet à Compose de réagir quand la valeur change.
    var telephone by remember { mutableStateOf("") }
    var codeSMS by remember { mutableStateOf("") }

    // Indique si Firebase a déjà envoyé le code par SMS.
    var codeEnvoye by remember { mutableStateOf(false) }

    // Indique si une opération est en cours.
    // Cela permet notamment d'éviter plusieurs clics simultanés.
    var chargement by remember { mutableStateOf(false) }

    // Message affiché pour informer l'utilisateur du résultat de l'opération.
    var message by remember { mutableStateOf("") }

    // Indique si le message doit être affiché comme une erreur.
    var erreur by remember { mutableStateOf(false) }

    // Permet d'exécuter des opérations suspendues, comme les lectures Firestore.
    val scope = rememberCoroutineScope()

    // Vérifie que le compte connecté possède bien un profil client.
    // Appelée après la vérification du code SMS,
    // ou quand Firebase connecte automatiquement le client.
    suspend fun verifierProfilClient(uid: String) {
        chargement = true
        message = "Vérification du profil client..."
        erreur = false

        // Recherche le profil correspondant à cet UID
        // dans la collection Firestore "utilisateurs".
        val resultatProfil =
            clientRepository.recupererProfilClient(uid)

        if (resultatProfil.isSuccess) {
            val profil = resultatProfil.getOrNull()

            // Le profil doit exister et avoir le rôle "client".
            if (profil != null && profil.role == "client") {
                // La connexion et la vérification du profil
                // ont réussi : on transmet l'UID à MainActivity.
                chargement = false
                onConnexionReussie(uid)
            } else {
                // Aucun profil client valide :
                // on déconnecte le compte Firebase.
                authRepository.deconnecter()
                chargement = false
                message =
                    "Aucun profil client trouvé. Crée d'abord un compte client."
                erreur = true
            }
        } else {
            // La lecture Firestore a échoué.
            // On ne considère pas le profil comme vérifié.
            chargement = false
            message =
                "Impossible de vérifier ton profil. Vérifie ta connexion Internet."
            erreur = true
        }
    }

    GabaritAuth(
        titre = if (codeEnvoye) "Vérification" else "Connexion",
        sousTitre = if (codeEnvoye) {
            "Saisis le code à 6 chiffres envoyé au ${telephone.trim()}."
        } else {
            "Entre ton numéro de téléphone pour recevoir un code de connexion par SMS."
        },
        icone = if (codeEnvoye) Icons.Filled.Lock else Icons.Filled.Sms,
        onRetour = onRetourAccueil,
        retourActif = !chargement
    ) {

        // Champ dans lequel le client saisit son numéro de téléphone.
        ChampTexte(
            valeur = telephone,
            onValeurChange = { telephone = it },
            libelle = "Numéro de téléphone",
            icone = Icons.Filled.Phone,
            exemple = "+225 07 00 00 00 00",
            actif = !codeEnvoye && !chargement,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            )
        )

        // Tant que le code n'a pas été envoyé, on affiche le bouton d'envoi.
        if (!codeEnvoye) {
            BoutonPrincipal(
                texte = "Recevoir un code SMS",
                chargement = chargement,
                onClick = {
                    // Vérifie que le numéro n'est pas vide et commence par +.
                    if (
                        telephone.isBlank() ||
                        !telephone.trim().startsWith("+")
                    ) {
                        message =
                            "Saisis ton numéro avec l'indicatif, par exemple +225."
                        erreur = true

                        // Arrête le traitement de ce clic.
                        return@BoutonPrincipal
                    }

                    // Informe l'utilisateur que l'envoi est en cours.
                    chargement = true
                    message = ""
                    erreur = false

                    // Demande à Firebase Authentication d'envoyer le SMS.
                    authRepository.envoyerCodeSMS(
                        activity = activity,
                        telephone = telephone.trim(),

                        // Cette fonction est appelée si l'envoi réussit.
                        onCodeEnvoye = {
                            codeEnvoye = true
                            chargement = false
                            message =
                                "Code envoyé. Saisis le code reçu par SMS."
                            erreur = false
                        },

                        // Firebase a vérifié le numéro sans code à saisir.
                        onConnexionAutomatique = { uid ->
                            scope.launch {
                                verifierProfilClient(uid)
                            }
                        },

                        // Cette fonction est appelée si Firebase rencontre une erreur.
                        onErreur = { detail ->
                            chargement = false
                            message = "Erreur Firebase : $detail"
                            erreur = true
                        }
                    )
                }
            )
        } else {
            // Le code a été envoyé : le client peut maintenant le saisir.
            ChampTexte(
                valeur = codeSMS,

                // Ne conserve que les chiffres, avec une longueur maximale de 6.
                onValeurChange = {
                    codeSMS = it
                        .filter { caractere -> caractere.isDigit() }
                        .take(6)
                },

                libelle = "Code de vérification",
                icone = Icons.Filled.Lock,
                exemple = "123456",
                actif = !chargement,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            BoutonPrincipal(
                texte = "Vérifier le code",
                chargement = chargement,
                onClick = {
                    // Firebase utilise ici un code de vérification à 6 chiffres.
                    if (codeSMS.length != 6) {
                        message = "Saisis le code à 6 chiffres."
                        erreur = true
                        return@BoutonPrincipal
                    }

                    // Lance une coroutine pour effectuer les opérations asynchrones.
                    scope.launch {
                        chargement = true
                        message = ""
                        erreur = false

                        // Vérifie le code auprès de Firebase Authentication.
                        // En cas de succès, le résultat contient l'UID du compte.
                        val resultatAuth =
                            authRepository.verifierCodeSMS(codeSMS)

                        if (resultatAuth.isSuccess) {
                            // Récupère l'identifiant unique du compte connecté,
                            // puis vérifie son profil client.
                            verifierProfilClient(resultatAuth.getOrThrow())
                        } else {
                            // Le code SMS est invalide ou la connexion a échoué.
                            chargement = false
                            message =
                                "Échec de la connexion : " +
                                        (
                                                resultatAuth.exceptionOrNull()
                                                    ?.localizedMessage
                                                    ?: "Vérifie le code SMS."
                                                )
                            erreur = true
                        }
                    }
                }
            )

            // Permet de corriger le numéro saisi.
            TextButton(
                onClick = {
                    codeEnvoye = false
                    codeSMS = ""
                    message = ""
                },
                enabled = !chargement
            ) {
                Text("Modifier le numéro")
            }
        }

        // Affiche le message de résultat, en rouge si c'est une erreur.
        if (message.isNotBlank()) {
            BandeauMessage(message = message, erreur = erreur)
        }
    }
}
