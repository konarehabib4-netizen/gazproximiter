package com.example.gazproximite.ui.composants

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gazproximite.R
import com.example.gazproximite.data.model.Depot

// Photo du dépôt (champ "photoUrl" dans Firestore).
// Sans photo, ou si elle ne se charge pas, on affiche l'illustration d'un entrepôt.
@Composable
fun PhotoDepot(
    depot: Depot,
    forme: Shape,
    modifier: Modifier = Modifier
) {
    val illustration = painterResource(R.drawable.ill_depot_gaz)

    Box(modifier = modifier.clip(forme)) {
        if (depot.photoUrl.isBlank()) {
            Image(
                painter = illustration,
                contentDescription = "Illustration du dépôt",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = depot.photoUrl,
                contentDescription = "Photo du dépôt ${depot.nom}",
                placeholder = illustration,
                error = illustration,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Signale clairement les dépôts fictifs.
        if (depot.estSimulation) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                BadgeSimulation()
            }
        }
    }
}

// Pastille "Simulation" pour les dépôts fictifs.
@Composable
fun BadgeSimulation() {
    Pastille(
        texte = "Simulation",
        couleurTexte = Color.White,
        couleurFond = Color(0xFF1565C0),
        icone = Icons.Filled.Science
    )
}
