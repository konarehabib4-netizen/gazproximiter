package com.example.gazproximite.ui.ecrans

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.ui.composants.CarteDepotAccueil

// Nombre de dépôts affichés sur l'accueil.
private const val NOMBRE_DEPOTS_ACCUEIL = 5

// Onglet "Accueil" de l'espace client :
// affiche les dépôts les plus proches du client.
@Composable
fun AccueilClientScreen(
    context: Context,
    onDepotClique: (DepotAvecDistance) -> Unit
) {
    val etat = rememberDepotsProches(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bonjour 👋",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Voici les dépôts de gaz les plus proches de vous.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {

            // Les dépôts sont en cours de récupération.
            etat.chargement -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Recherche des dépôts proches...")
                }
            }

            // Aucun dépôt n'a été récupéré.
            etat.depots.isEmpty() -> {
                Text(
                    text = etat.messageErreur.ifBlank {
                        "Aucun dépôt disponible pour le moment."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Affiche les dépôts les plus proches.
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = etat.depots.take(NOMBRE_DEPOTS_ACCUEIL),
                        key = { depotAvecDistance ->
                            depotAvecDistance.depot.id
                        }
                    ) { depotAvecDistance ->
                        CarteDepotAccueil(
                            depotAvecDistance = depotAvecDistance,
                            onClick = {
                                onDepotClique(depotAvecDistance)
                            }
                        )
                    }
                }
            }
        }
    }
}
