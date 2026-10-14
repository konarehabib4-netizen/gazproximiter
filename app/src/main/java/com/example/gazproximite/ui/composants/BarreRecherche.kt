package com.example.gazproximite.ui.composants

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Champ de recherche des dépôts.
@Composable
fun BarreRecherche(
    recherche: String,
    onRechercheChange: (String) -> Unit
) {
    OutlinedTextField(
        value = recherche,
        onValueChange = onRechercheChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text("Rechercher un dépôt")
        },
        placeholder = {
            Text("Nom, commune, marque, format...")
        },
        singleLine = true
    )
}
