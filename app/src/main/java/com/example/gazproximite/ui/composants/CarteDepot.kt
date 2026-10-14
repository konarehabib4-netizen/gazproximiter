package com.example.gazproximite.ui.composants

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gazproximite.R
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.DepotAvecDistance

// Carte complète d'un dépôt, utilisée dans la liste des dépôts.
@Composable
fun CarteDepot(
    depotAvecDistance: DepotAvecDistance,
    context: Context,
    onClick: () -> Unit = {}
) {
    val depot = depotAvecDistance.depot

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            EnTeteCarteDepot(
                depot = depot,
                distance = formaterDistance(depotAvecDistance.distanceMetres)
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DisponibiliteBouteilles(stock = depot.stock)

            Spacer(modifier = Modifier.height(12.dp))

            BoutonsActionDepot(depot = depot, context = context)
        }
    }
}

// Haut de carte : vignette, nom, commune, distance et statut.
@Composable
fun EnTeteCarteDepot(
    depot: Depot,
    distance: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        VignetteDepot()

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = depot.nom.ifBlank { "Dépôt de gaz" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )

                Spacer(modifier = Modifier.width(3.dp))

                Text(
                    text = "${depot.commune.ifBlank { "Commune inconnue" }} · $distance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            StatutOuverture(estOuvert = depot.estOuvert)
        }
    }
}

// Petite image carrée représentant le dépôt.
@Composable
fun VignetteDepot() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bouteille_gaz),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )
    }
}

// Boutons "Appeler" et "Itinéraire".
@Composable
fun BoutonsActionDepot(
    depot: Depot,
    context: Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (depot.telephone.isNotBlank()) {
            Button(
                onClick = { appelerDepot(context, depot.telephone) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Appeler")
            }
        }

        if (depot.position != null) {
            OutlinedButton(
                onClick = { ouvrirItineraire(context, depot) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Itinéraire")
            }
        }
    }
}
