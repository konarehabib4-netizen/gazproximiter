package com.example.gazproximite.ui.ecrans

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
            when (ongletSelectionne) {

                // ACCUEIL : affiche les dépôts les plus proches.
                OngletClient.ACCUEIL -> {
                    val depot = depotSelectionne

                    if (depot != null) {
                        DetailDepotScreen(
                            depotAvecDistance = depot,
                            context = context,
                            onRetour = {
                                depotSelectionne = null
                            }
                        )
                    } else {
                        AccueilClientScreen(
                            context = context,
                            onDepotClique = { depotClique ->
                                depotSelectionne = depotClique
                            }
                        )
                    }
                }

                // DÉPÔTS : liste complète avec recherche.
                OngletClient.DEPOTS -> {
                    DepotsScreen(context = context)
                }

                // FAVORIS : écran provisoire.
                OngletClient.FAVORIS -> {
                    FavorisScreen()
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
