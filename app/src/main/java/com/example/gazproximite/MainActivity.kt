
package com.example.gazproximite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Les classes qui permettent de communiquer avec Firebase.
import com.example.gazproximite.data.repository.ClientAuthRepository
import com.example.gazproximite.data.repository.ClientRepository

// Les écrans de notre application.
import ui.ecrans.AccueilScreen
import ui.ecrans.ConnexionClientScreen
import ui.ecrans.InscriptionClientScreen
import ui.ecrans.DepotsScreen

// Le thème graphique de GazProximité.
import com.example.gazproximite.ui.theme.GazproximiteTheme


// MainActivity est le point d'entrée principal de notre application Android.
class MainActivity : ComponentActivity() {

    // Ce repository gère l'authentification Firebase :
    // envoi du code SMS, vérification du code et déconnexion.
    private val authRepository = ClientAuthRepository()

    // Ce repository gère les données du profil client dans Firestore.
    private val clientRepository = ClientRepository()

    // onCreate est appelée lorsque l'activité est créée par Android.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permet à l'interface de s'étendre derrière les barres système.
        enableEdgeToEdge()

        // setContent affiche l'interface construite avec Jetpack Compose.
        setContent {

            // Applique les couleurs, les textes et le style de notre application.
            GazproximiteTheme {

                // Mémorise le nom de l'écran actuellement affiché.
                // Au démarrage, l'application affiche l'accueil.
                var ecranActuel by remember {
                    mutableStateOf("accueil")
                }

                // Selon la valeur de ecranActuel, on affiche le bon écran.
                // Quand cette valeur change, Compose actualise l'interface.
                when (ecranActuel) {

                    // ÉCRAN 1 : ACCUEIL
                    "accueil" -> {
                        AccueilScreen(

                            // Si l'utilisateur appuie sur "Se connecter",
                            // on affiche l'écran de connexion.
                            onSeConnecter = {
                                ecranActuel = "connexion"
                            },

                            // Si l'utilisateur appuie sur "Créer un compte",
                            // on affiche l'écran d'inscription.
                            onCreerCompte = {
                                ecranActuel = "inscription"
                            }
                        )
                    }

                    // ÉCRAN 2 : CONNEXION
                    "connexion" -> {
                        ConnexionClientScreen(

                            // L'activité est nécessaire à Firebase
                            // pour envoyer le SMS de vérification.
                            activity = this,

                            // Fournit le repository d'authentification.
                            authRepository = authRepository,

                            // Fournit le repository qui lit le profil Firestore.
                            clientRepository = clientRepository,

                            // Si l'utilisateur revient en arrière vers l'accueil,
                            // on change simplement l'écran affiché.
                            onRetourAccueil = {
                                ecranActuel = "accueil"
                            },

                            // Ce callback est appelé lorsque le code SMS
                            // est valide et que le profil client est vérifié.
                            onConnexionReussie = { uid ->

                                // uid est l'identifiant unique du client.
                                // Nous pourrons l'utiliser plus tard pour
                                // récupérer ses informations personnelles.
                                ecranActuel = "depots"
                            }
                        )
                    }

                    // ÉCRAN 3 : INSCRIPTION
                    "inscription" -> {
                        InscriptionClientScreen(

                            // Nécessaire pour l'authentification par SMS.
                            activity = this,

                            // Permet d'envoyer et de vérifier le code SMS.
                            authRepository = authRepository,

                            // Permet d'enregistrer le profil du nouveau client.
                            clientRepository = clientRepository,

                            // Après avoir appuyé sur "Retour à l'accueil",
                            // on revient à l'écran de départ.
                            onRetourAccueil = {
                                ecranActuel = "accueil"
                            }
                        )
                    }

                    // ÉCRAN 4 : LISTE DES DÉPÔTS
                    "depots" -> {
                        DepotsScreen(

                            // Le contexte Android est nécessaire pour accéder
                            // aux services de localisation du téléphone.
                            context = this,

                            // Quand le client se déconnecte :
                            onDeconnexion = {

                                // Ferme la session Firebase.
                                authRepository.deconnecter()

                                // Puis ramène le client à l'accueil.
                                ecranActuel = "accueil"
                            }
                        )
                    }
                }
            }
        }
    }
}
