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
import com.example.gazproximite.data.repository.VendeurRepository
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.ui.vendeur.ConnexionVendeurScreen
import com.example.gazproximite.ui.vendeur.EspaceVendeurScreen
import com.example.gazproximite.ui.vendeur.InscriptionVendeurScreen
import androidx.compose.runtime.LaunchedEffect
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

    // Gère l'espace vendeur (connexion par e-mail, stock, ventes).
    private val vendeurRepository = VendeurRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GazproximiteTheme {

                // Écran affiché actuellement.
                var ecranActuel by remember {
                    mutableStateOf("accueil")
                }

                // Vendeur connecté (pour l'espace vendeur).
                var vendeur by remember {
                    mutableStateOf<Utilisateur?>(null)
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
                            },
                            onEspaceVendeur = {
                                ecranActuel = "connexion_vendeur"
                            }
                        )
                    }

                    // Connexion du vendeur par e-mail.
                    "connexion_vendeur" -> {
                        // Un vendeur déjà connecté entre directement dans son espace.
                        LaunchedEffect(Unit) {
                            vendeurRepository.vendeurConnecte()?.let { connecte ->
                                vendeur = connecte
                                ecranActuel = "espace_vendeur"
                            }
                        }

                        ConnexionVendeurScreen(
                            vendeurRepository = vendeurRepository,
                            onRetour = { ecranActuel = "accueil" },
                            onConnexionReussie = { connecte ->
                                vendeur = connecte
                                ecranActuel = "espace_vendeur"
                            },
                            onCreerDepot = { ecranActuel = "inscription_vendeur" }
                        )
                    }

                    // Création d'un compte vendeur et de son dépôt.
                    "inscription_vendeur" -> {
                        InscriptionVendeurScreen(
                            context = this,
                            vendeurRepository = vendeurRepository,
                            onRetour = { ecranActuel = "connexion_vendeur" },
                            onInscriptionReussie = { inscrit ->
                                vendeur = inscrit
                                ecranActuel = "espace_vendeur"
                            }
                        )
                    }

                    // Espace du vendeur : stock, ventes, dépôt.
                    "espace_vendeur" -> {
                        val vendeurConnecte = vendeur
                        if (vendeurConnecte == null) {
                            LaunchedEffect(Unit) { ecranActuel = "connexion_vendeur" }
                        } else {
                            EspaceVendeurScreen(
                                context = this,
                                vendeur = vendeurConnecte,
                                vendeurRepository = vendeurRepository,
                                onDeconnexion = {
                                    vendeurRepository.deconnecter()
                                    vendeur = null
                                    ecranActuel = "accueil"
                                }
                            )
                        }
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
