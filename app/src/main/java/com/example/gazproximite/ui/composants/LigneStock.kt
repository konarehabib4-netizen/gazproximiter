package com.example.gazproximite.ui.composants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.cleStock
import com.example.gazproximite.ui.theme.RougeRupture
import com.example.gazproximite.ui.theme.VertDisponible

// Affiche un format de bouteille et sa disponibilité.
@Composable
fun LigneStock(
    format: String,
    disponible: Boolean?
) {
    // Détermine le texte selon la valeur enregistrée dans Firebase.
    val texte = when (disponible) {
        true -> "En stock"
        false -> "Rupture"
        null -> "Non renseigné"
    }

    // Détermine la couleur de l'état.
    val couleur = when (disponible) {
        true -> VertDisponible
        false -> RougeRupture
        null -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = format)

        Text(
            text = texte,
            color = couleur
        )
    }
}

// Affiche la disponibilité de toutes les bouteilles d'un dépôt :
// pour chaque marque, une ligne par format (B6, B12, B28).
@Composable
fun DisponibiliteBouteilles(
    stock: Map<String, Boolean>
) {
    Column {
        Marque.entries.forEach { marque ->
            Text(
                text = marque.libelle,
                style = MaterialTheme.typography.titleSmall
            )

            Format.entries.forEach { format ->
                LigneStock(
                    format = format.name,
                    disponible = stock[cleStock(marque, format)]
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

// Affiche "● Ouvert" en vert ou "● Fermé" en rouge.
@Composable
fun StatutOuverture(
    estOuvert: Boolean
) {
    Text(
        text = if (estOuvert) "● Ouvert" else "● Fermé",
        color = if (estOuvert) VertDisponible else RougeRupture,
        style = MaterialTheme.typography.labelLarge
    )
}
