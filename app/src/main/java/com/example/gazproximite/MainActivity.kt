package com.example.gazproximite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.example.gazproximite.data.repository.ClientAuthRepository
import com.example.gazproximite.data.repository.ClientRepository
import com.example.gazproximite.ui.theme.GazproximiteTheme

import com.example.gazproximite.ui.ecrans.AccueilScreen
import com.example.gazproximite.ui.ecrans.ConnexionClientScreen
import com.example.gazproximite.ui.ecrans.InscriptionClientScreen
import com.example.gazproximite.ui.ecrans.EspaceClientScreen

class MainActivity : ComponentActivity() {

    // Gère l'authentification du client par SMS.
    private val authRepository = ClientAuthRepository()

    // Gère le profil client enregistré dans Firestore.
    private val clientRepository = ClientRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GazproximiteTheme {

                // Écran affiché actuellement.
                var ecranActuel by remember {
                    mutableStateOf("accueil")
                }

                when (ecranActuel) {

                    // Écran de présentation avant la connexion.
                    "accueil" -> {
                        AccueilScreen(
                            onSeConnecter = {
                                ecranActuel = "connexion"
                            },
                            onCreerCompte = {
                                ecranActuel = "inscription"
                            }
                        )
                    }

                    // Connexion du client par SMS.
                    "connexion" -> {
                        ConnexionClientScreen(
                            activity = this,
                            authRepository = authRepository,
                            clientRepository = clientRepository,
                            onRetourAccueil = {
                                ecranActuel = "accueil"
                            },
                            onConnexionReussie = { _ ->
                                // Après la connexion, ouvrir l'espace client.
                                ecranActuel = "espace_client"
                            }
                        )
                    }

                    // Inscription du nouveau client.
                    "inscription" -> {
                        InscriptionClientScreen(
                            activity = this,
                            authRepository = authRepository,
                            clientRepository = clientRepository,
                            onRetourAccueil = {
                                ecranActuel = "accueil"
                            }
                        )
                    }

                    // Espace privé accessible après la connexion.
                    "espace_client" -> {
                        EspaceClientScreen(
                            context = this,
                            onDeconnexion = {
                                authRepository.deconnecter()

                                // Retour à l'écran de départ après déconnexion.
                                ecranActuel = "accueil"
                            }
                        )
                    }
                }
            }
        }
    }
}
