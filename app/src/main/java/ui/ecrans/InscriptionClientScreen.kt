package ui.ecrans
// Permet de transmettre l'activité Android à Firebase
// pour gérer l'authentification par téléphone.
import android.app.Activity

// Composants de mise en page : colonne, espaces et défilement.
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Composants visuels Material 3.
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text

// Outils Compose pour gérer les états de l'écran.
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

// Outils pour le clavier.
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

// Outils pour définir les dimensions.
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Nos classes qui communiquent avec Firebase.
import com.example.gazproximite.data.repository.ClientAuthRepository
import com.example.gazproximite.data.repository.ClientRepository

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

                // Identifiant unique du compte Firebase.
                uid = uid,

                // Informations provenant du formulaire.
                nom = nom,
                prenom = prenom,
                adresse = adresse,

                // On conserve le numéro au format international.
                telephone = telephone.trim()
            )

            // Si Firestore a enregistré les données.
            resultat.onSuccess {

                inscriptionTerminee = true

                message = "Inscription réussie ! Profil enregistré."
            }

                // Si Firestore a rencontré une erreur.
                .onFailure { erreur ->

                    // Afficher la cause de l'échec.
                    message = "Erreur Firestore : ${
                        erreur.localizedMessage ?: "erreur inconnue"
                    }"
                }

            // L'opération est terminée.
            chargement = false
        }
    }


    // =====================================================
    // 4. CONSTRUCTION DE L'INTERFACE
    // =====================================================

    Column(

        // L'écran occupe toute la place disponible.
        // Le défilement permet d'accéder aux champs
        // même si l'écran est petit ou le clavier ouvert.
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),

        // Espacement vertical entre les éléments.
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Titre principal de l'application.
        Text(
            text = "GazProximité",
            style = MaterialTheme.typography.headlineMedium
        )

        // Sous-titre de l'écran.
        Text(
            text = "Créer un compte client",
            style = MaterialTheme.typography.titleLarge
        )


        // -------------------------------------------------
        // CHAMP NOM
        // -------------------------------------------------

        OutlinedTextField(
            value = nom,

            // Chaque saisie met à jour la variable nom.
            onValueChange = { nom = it },

            label = { Text("Nom") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,

            // Après l'envoi du code, on bloque la modification
            // des informations pour conserver la cohérence du formulaire.
            enabled = !codeEnvoye && !inscriptionTerminee
        )


        // -------------------------------------------------
        // CHAMP PRÉNOM
        // -------------------------------------------------

        OutlinedTextField(
            value = prenom,
            onValueChange = { prenom = it },
            label = { Text("Prénom") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !codeEnvoye && !inscriptionTerminee
        )


        // -------------------------------------------------
        // CHAMP TÉLÉPHONE
        // -------------------------------------------------

        OutlinedTextField(
            value = telephone,
            onValueChange = { telephone = it },
            label = { Text("Téléphone avec indicatif") },

            // Exemple de format attendu.
            placeholder = { Text("+225XXXXXXXXXX") },

            modifier = Modifier.fillMaxWidth(),
            singleLine = true,

            // Demande au téléphone d'afficher un clavier adapté.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),

            enabled = !codeEnvoye && !inscriptionTerminee
        )


        // -------------------------------------------------
        // CHAMP ADRESSE FACULTATIF
        // -------------------------------------------------

        OutlinedTextField(
            value = adresse,
            onValueChange = { adresse = it },
            label = { Text("Adresse (facultative)") },
            placeholder = { Text("Ex. : Cocody, Angré") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !codeEnvoye && !inscriptionTerminee
        )


        // =================================================
        // 5. BOUTON D'ENVOI DU CODE SMS
        // =================================================

        // Ce bouton apparaît tant que le code n'a pas été envoyé.
        if (!codeEnvoye) {

            Button(

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

                        // Arrêter le traitement de ce clic.
                        return@Button
                    }

                    // Vérifier la présence du signe + international.
                    if (!telephone.trim().startsWith("+")) {
                        message =
                            "Ajoute l'indicatif du pays, par exemple +225."

                        return@Button
                    }

                    // Informer le client et afficher le chargement.
                    message = "Demande d'envoi du code..."
                    chargement = true

                    // Demander à Firebase d'envoyer le code.
                    authRepository.envoyerCodeSMSPourInscription(

                        activity = activity,
                        telephone = telephone.trim(),

                        // Appelé lorsque Firebase confirme l'envoi du code.
                        onCodeEnvoye = {

                            // Afficher le champ de saisie du code.
                            codeEnvoye = true

                            chargement = false

                            message =
                                "Code envoyé. Saisis le code reçu."
                        },

                        // Firebase peut parfois vérifier automatiquement
                        // le numéro, sans demander au client de saisir le code.
                        onConnexionAutomatique = { uid ->

                            // Enregistrer le profil avec l'UID reçu.
                            enregistrerProfil(uid)
                        },

                        // Appelé si l'envoi du code échoue.
                        onErreur = { erreur ->

                            chargement = false
                            message = "Erreur Firebase : $erreur"
                        }
                    )
                },

                modifier = Modifier.fillMaxWidth(),

                // Empêche les clics répétés pendant le chargement.
                enabled = !chargement && !inscriptionTerminee

            ) {
                Text("Créer mon compte")
            }
        }


        // =================================================
        // 6. CHAMP ET BOUTON DE VÉRIFICATION DU CODE
        // =================================================

        // Cette partie s'affiche après l'envoi du code.
        else if (!inscriptionTerminee) {

            OutlinedTextField(
                value = codeSMS,

                // Ne conserver que les chiffres, avec un maximum
                // de six caractères.
                onValueChange = {
                    codeSMS = it
                        .filter { caractere -> caractere.isDigit() }
                        .take(6)
                },

                label = { Text("Code de vérification") },
                placeholder = { Text("123456") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,

                // Afficher un clavier numérique.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )


            Button(

                onClick = {

                    // Le code de test Firebase comporte six chiffres.
                    if (codeSMS.length != 6) {
                        message = "Saisis le code à 6 chiffres."
                        return@Button
                    }

                    // Vérifier le code auprès de Firebase.
                    scope.launch {

                        chargement = true
                        message = "Vérification du code..."

                        val resultat =
                            authRepository.verifierCodeSMS(codeSMS)

                        // Si le code est valide, Firebase renvoie l'UID.
                        resultat.onSuccess { uid ->

                            // Créer le profil dans Firestore.
                            enregistrerProfil(uid)
                        }

                            // Si le code est invalide ou expiré.
                            .onFailure { erreur ->

                                chargement = false

                                message = "Code invalide ou expiré : ${
                                    erreur.localizedMessage
                                        ?: "vérifie le code"
                                }"
                            }
                    }
                },

                modifier = Modifier.fillMaxWidth(),

                // Désactiver le bouton pendant la vérification.
                enabled = !chargement && !inscriptionTerminee

            ) {
                Text("Vérifier le code et terminer")
            }
        }


        // =================================================
        // 7. AFFICHER LE CHARGEMENT
        // =================================================

        // Le cercle tourne pendant une opération en cours.
        if (chargement) {
            CircularProgressIndicator()
        }


        // =================================================
        // 8. AFFICHER LE MESSAGE DE RÉSULTAT
        // =================================================

        if (message.isNotBlank()) {

            Text(
                text = message,

                // Utiliser la couleur principale en cas de succès,
                // sinon la couleur d'erreur.
                color = if (inscriptionTerminee) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },

                style = MaterialTheme.typography.bodyMedium
            )
        }


        // =================================================
        // 9. CONFIRMATION DE L'INSCRIPTION
        // =================================================

        if (inscriptionTerminee) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ton compte client est prêt.",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

