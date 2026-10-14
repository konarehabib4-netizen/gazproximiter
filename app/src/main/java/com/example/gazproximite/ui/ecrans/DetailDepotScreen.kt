package com.example.gazproximite.ui.ecrans

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.ui.composants.DisponibiliteBouteilles
import com.example.gazproximite.ui.composants.StatutOuverture
import com.example.gazproximite.ui.composants.appelerDepot

// Détail d'un dépôt : horaires, stock complet et bouton d'appel.
@Composable
fun DetailDepotScreen(
    depotAvecDistance: DepotAvecDistance,
    context: Context,
    onRetour: () -> Unit
) {
    val depot = depotAvecDistance.depot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Button(onClick = onRetour) {
            Text("← Retour")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = depot.nom,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Commune : ${depot.commune}")
        Text("Distance : ${formaterDistance(depotAvecDistance.distanceMetres)}")

        Spacer(modifier = Modifier.height(8.dp))

        StatutOuverture(estOuvert = depot.estOuvert)

        if (depot.heureOuverture.isNotBlank()) {
            Text("Ouverture : ${depot.heureOuverture}")
        }

        if (depot.heureFermeture.isNotBlank()) {
            Text("Fermeture : ${depot.heureFermeture}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Disponibilité des bouteilles",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        DisponibiliteBouteilles(stock = depot.stock)

        if (depot.telephone.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    appelerDepot(context, depot.telephone)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Appeler le dépôt")
            }
        }
    }
}
