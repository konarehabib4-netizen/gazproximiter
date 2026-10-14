package com.example.gazproximite.ui.composants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.DepotAvecDistance

// Carte résumée d'un dépôt, affichée sur l'accueil du client.
// Un clic ouvre le détail du dépôt.
@Composable
fun CarteDepotAccueil(
    depotAvecDistance: DepotAvecDistance,
    onClick: () -> Unit
) {
    val depot = depotAvecDistance.depot

    // Compter les formats marqués disponibles dans Firestore.
    val nombreDisponibles = depot.stock.count { (_, disponible) ->
        disponible
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⛽",
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = depot.nom.ifBlank {
                            "Dépôt de gaz"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = depot.commune,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = formaterDistance(depotAvecDistance.distanceMetres),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            StatutOuverture(estOuvert = depot.estOuvert)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$nombreDisponibles format(s) marqué(s) en stock",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Consulter les stocks et les horaires →",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
