package com.example.gazproximite.ui.ecrans

// Permet de transmettre l'activité Android à Firebase
// pour gérer l'authentification par téléphone.
import android.app.Activity

// Icônes Material.
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone

// Outils Compose pour gérer les états de l'écran.
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

// Outils pour le clavier.
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

// Nos classes qui communiquent avec Firebase.
import com.example.gazproximite.data.repository.ClientAuthRepository
import com.example.gazproximite.data.repository.ClientRepository

// Composants visuels de l'application.
import com.example.gazproximite.ui.composants.BandeauMessage
import com.example.gazproximite.ui.composants.ChampTexte
import com.example.gazproximite.ui.composants.EtatVide

// Permet d'exécuter des opérations suspend sans bloquer l'écran.
import kotlinx.coroutines.launch


// @Composable indique que cette fonction construit une interface Compose.
@Composable
fun InscriptionClientScreen(
    activity: Activity,
    authRepository: ClientAuthRepository,
    clientRepository: ClientRepository,
    onRetourAccueil: () -> Unit = {}
) {

    // =====================================================
    // 1. INFORMATIONS SAISIES PAR LE CLIENT
    // =====================================================

    // remember conserve la valeur pendant les recompositions
    // de l'écran. mutableStateOf permet à Compose de réagir
    // lorsque la valeur change.
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }

    // Ce champ contiendra le code de vérification reçu par SMS.
    var codeSMS by remember { mutableStateOf("") }


    // =====================================================
    // 2. ÉTATS DE L'INSCRIPTION
    // =====================================================

    // Indique si Firebase a envoyé le code.
    var codeEnvoye by remember { mutableStateOf(false) }

    // Indique si une opération est en cours.
    // Par exemple : envoi du code ou enregistrement Firestore.
    var chargement by remember { mutableStateOf(false) }

    // Message destiné à informer le client du résultat.
    var message by remember { mutableStateOf("") }

    // Indique si le message doit être affiché comme une erreur.
    var erreur by remember { mutableStateOf(false) }

    // Indique que le profil a été enregistré avec succès.
    var inscriptionTerminee by remember { mutableStateOf(false) }


    // Permet de lancer des opérations asynchrones
    // sans bloquer l'interface graphique.
    val scope = rememberCoroutineScope()


    // =====================================================
    // 3. FONCTION POUR ENREGISTRER LE PROFIL
    // =====================================================

    // Cette fonction est appelée après la vérification du numéro.
    // Elle reçoit l'UID attribué par Firebase Authentication.
    fun enregistrerProfil(uid: String) {

        // L'enregistrement Firestore est une opération suspend.
        // launch permet de l'exécuter depuis l'écran Compose.
        scope.launch {

            // Afficher l'état de chargement.
            chargement = true

            // Appeler notre repository pour enregistrer les données.
            val resultat = clientRepository.creerProfilClient(
                uid = uid,
                nom = nom,
                prenom = prenom,
                adresse = adresse,

                // On conserve le numéro au format international.
                telephone = telephone.trim()
            )

            // Si Firestore a enregistré les données.
            resultat.onSuccess {
                inscriptionTerminee = true
                message = ""
                erreur = false
            }

                // Si Firestore a rencontré une erreur.
                .onFailure { e ->
                    message = "Erreur Firestore : ${
                        e.localizedMessage ?: "erreur inconnue"
                    }"
                    erreur = true
                }

            // L'opération est terminée.
            chargement = false
        }
    }


    // =====================================================
    // 4. INSCRIPTION TERMINÉE
    // =====================================================

    if (inscriptionTerminee) {
        EtatVide(
            icone = Icons.Filled.CheckCircle,
            titre = "Bienvenue ${prenom.trim()} !",
            description = "Ton compte client est prêt. " +
                    "Connecte-toi pour trouver les dépôts de gaz proches de toi."
        ) {
            BoutonPrincipal(
                texte = "Retour à l'accueil pour se connecter",
                onClick = onRetourAccueil
            )
        }
        return
    }


    // =====================================================
    // 5. FORMULAIRE
    // =====================================================

    // Après l'envoi du code, on bloque la modification
    // des informations pour conserver la cohérence du formulaire.
    val champsActifs = !codeEnvoye && !chargement

    GabaritAuth(
        titre = "Créer un compte",
        sousTitre = "Quelques informations pour trouver ton gaz plus vite.",
        icone = Icons.Filled.PersonAdd,
        onRetour = onRetourAccueil,
        retourActif = !chargement
    ) {

        ChampTexte(
            valeur = nom,
            onValeurChange = { nom = it },
            libelle = "Nom",
            icone = Icons.Filled.Badge,
            actif = champsActifs,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            )
        )

        ChampTexte(
            valeur = prenom,
            onValeurChange = { prenom = it },
            libelle = "Prénom",
            icone = Icons.Filled.Person,
            actif = champsActifs,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            )
        )

        ChampTexte(
            valeur = telephone,
            onValeurChange = { telephone = it },
            libelle = "Téléphone avec indicatif",
            icone = Icons.Filled.Phone,
            exemple = "+225 07 00 00 00 00",
            actif = champsActifs,

            // Demande au téléphone d'afficher un clavier adapté.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            )
        )

        ChampTexte(
            valeur = adresse,
            onValeurChange = { adresse = it },
            libelle = "Adresse (facultative)",
            icone = Icons.Filled.Home,
            exemple = "Ex. : Cocody, Angré",
            actif = champsActifs
        )


        // =================================================
        // 6. BOUTON D'ENVOI DU CODE SMS
        // =================================================

        // Ce bouton apparaît tant que le code n'a pas été envoyé.
        if (!codeEnvoye) {

            BoutonPrincipal(
                texte = "Créer mon compte",
                chargement = chargement,
                onClick = {

                    // Vérifier que les champs obligatoires sont remplis.
                    // isBlank détecte une chaîne vide ou composée d'espaces.
                    if (
                        nom.isBlank() ||
                        prenom.isBlank() ||
                        telephone.isBlank()
                    ) {
                        message =
                            "Renseigne le nom, le prénom et le téléphone."
                        erreur = true

                        // Arrêter le traitement de ce clic.
                        return@BoutonPrincipal
                    }

                    // Vérifier la présence du signe + international.
                    if (!telephone.trim().startsWith("+")) {
                        message =
                            "Ajoute l'indicatif du pays, par exemple +225."
                        erreur = true

                        return@BoutonPrincipal
                    }

                    // Afficher le chargement.
                    message = ""
                    chargement = true

                    // Demander à Firebase d'envoyer le code.
                    authRepository.envoyerCodeSMS(

                        activity = activity,
                        telephone = telephone.trim(),

                        // Appelé lorsque Firebase confirme l'envoi du code.
                        onCodeEnvoye = {
                            // Afficher le champ de saisie du code.
                            codeEnvoye = true
                            chargement = false
                            message = "Code envoyé. Saisis le code reçu par SMS."
                            erreur = false
                        },

                        // Firebase peut parfois vérifier automatiquement
                        // le numéro, sans demander au client de saisir le code.
                        onConnexionAutomatique = { uid ->
                            enregistrerProfil(uid)
                        },

                        // Appelé si l'envoi du code échoue.
                        onErreur = { detail ->
                            chargement = false
                            message = "Erreur Firebase : $detail"
                            erreur = true
                        }
                    )
                }
            )
        }


        // =================================================
        // 7. CHAMP ET BOUTON DE VÉRIFICATION DU CODE
        // =================================================

        // Cette partie s'affiche après l'envoi du code.
        else {

            ChampTexte(
                valeur = codeSMS,

                // Ne conserver que les chiffres, avec un maximum
                // de six caractères.
                onValeurChange = {
                    codeSMS = it
                        .filter { caractere -> caractere.isDigit() }
                        .take(6)
                },

                libelle = "Code de vérification",
                icone = Icons.Filled.Lock,
                exemple = "123456",
                actif = !chargement,

                // Afficher un clavier numérique.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            BoutonPrincipal(
                texte = "Vérifier le code et terminer",
                chargement = chargement,
                onClick = {

                    // Le code de test Firebase comporte six chiffres.
                    if (codeSMS.length != 6) {
                        message = "Saisis le code à 6 chiffres."
                        erreur = true
                        return@BoutonPrincipal
                    }

                    // Vérifier le code auprès de Firebase.
                    scope.launch {

                        chargement = true
                        message = ""

                        val resultat =
                            authRepository.verifierCodeSMS(codeSMS)

                        // Si le code est valide, Firebase renvoie l'UID.
                        resultat.onSuccess { uid ->

                            // Créer le profil dans Firestore.
                            enregistrerProfil(uid)
                        }

                            // Si le code est invalide ou expiré.
                            .onFailure { e ->

                                chargement = false

                                message = "Code invalide ou expiré : ${
                                    e.localizedMessage ?: "vérifie le code"
                                }"
                                erreur = true
                            }
                    }
                }
            )
        }


        // =================================================
        // 8. AFFICHER LE MESSAGE DE RÉSULTAT
        // =================================================

        if (message.isNotBlank()) {
            BandeauMessage(message = message, erreur = erreur)
        }
    }
}
