package com.example.gazproximite.ui.ecrans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Onglet "Favoris" : écran provisoire en attendant la fonctionnalité.
@Composable
fun FavorisScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mes favoris",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Les dépôts favoris seront affichés ici.",
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
