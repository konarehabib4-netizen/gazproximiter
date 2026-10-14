package com.example.gazproximite.ui.ecrans

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.ui.navigation.BottomBar
import com.example.gazproximite.ui.navigation.OngletClient

// Espace privé du client, accessible après la connexion.
@Composable
fun EspaceClientScreen(
    context: Context,
    onDeconnexion: () -> Unit
) {
    var ongletSelectionne by remember { mutableStateOf(OngletClient.ACCUEIL) }

    // Mémorise le dépôt choisi pour afficher ses détails.
    var depotSelectionne by remember {
        mutableStateOf<DepotAvecDistance?>(null)
    }

    // Le bouton "retour" du téléphone ferme le détail du dépôt.
    BackHandler(enabled = depotSelectionne != null) {
        depotSelectionne = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,

        // Les en-têtes passent derrière la barre d'état :
        // chaque écran gère lui-même la marge du haut.
        contentWindowInsets = WindowInsets(0),

        bottomBar = {
            BottomBar(
                ongletSelectionne = ongletSelectionne,
                onOngletClique = { onglet ->
                    ongletSelectionne = onglet
                    depotSelectionne = null
                }
            )
        }
    ) { paddingInterieur ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterieur)
        ) {
            val depot = depotSelectionne

            // Un dépôt choisi (depuis l'accueil ou la liste) affiche son détail.
            if (depot != null) {
                DetailDepotScreen(
                    depotAvecDistance = depot,
                    context = context,
                    onRetour = {
                        depotSelectionne = null
                    }
                )
            } else when (ongletSelectionne) {

                // ACCUEIL : affiche les dépôts les plus proches.
                OngletClient.ACCUEIL -> {
                    AccueilClientScreen(
                        context = context,
                        onDepotClique = { depotClique ->
                            depotSelectionne = depotClique
                        },
                        onVoirTout = {
                            ongletSelectionne = OngletClient.DEPOTS
                        }
                    )
                }

                // CARTE : dépôts sur une carte, avec mode simulation.
                OngletClient.CARTE -> {
                    CarteScreen(
                        context = context,
                        onDepotClique = { depotClique ->
                            depotSelectionne = depotClique
                        }
                    )
                }

                // DÉPÔTS : liste complète avec recherche.
                OngletClient.DEPOTS -> {
                    DepotsScreen(
                        context = context,
                        onDepotClique = { depotClique ->
                            depotSelectionne = depotClique
                        }
                    )
                }

                // FAVORIS : écran provisoire.
                OngletClient.FAVORIS -> {
                    FavorisScreen(
                        onExplorer = {
                            ongletSelectionne = OngletClient.DEPOTS
                        }
                    )
                }

                // MON COMPTE : profil et déconnexion.
                OngletClient.MON_COMPTE -> {
                    MonCompteScreen(
                        onDeconnexion = onDeconnexion
                    )
                }
            }
        }
    }
}
