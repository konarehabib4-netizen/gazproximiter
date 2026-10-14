package com.example.gazproximite.ui.composants

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.DepotAvecDistance

// Carte complète d'un dépôt, utilisée dans la liste des dépôts.
@Composable
fun CarteDepot(
    depotAvecDistance: DepotAvecDistance,
    context: Context
) {
    val depot = depotAvecDistance.depot

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = depot.nom,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text("Commune : ${depot.commune}")
            Text("Distance : ${formaterDistance(depotAvecDistance.distanceMetres)}")

            Spacer(modifier = Modifier.height(6.dp))

            StatutOuverture(estOuvert = depot.estOuvert)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Disponibilité des bouteilles",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            DisponibiliteBouteilles(stock = depot.stock)

            if (depot.telephone.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Appeler le dépôt : ${depot.telephone}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable {
                        appelerDepot(context, depot.telephone)
                    }
                )
            }
        }
    }
}

// Ouvre le composeur téléphonique avec le numéro du dépôt.
fun appelerDepot(context: Context, telephone: String) {
    val intent = Intent(
        Intent.ACTION_DIAL,
        Uri.fromParts("tel", telephone.trim(), null)
    )
    context.startActivity(intent)
}
