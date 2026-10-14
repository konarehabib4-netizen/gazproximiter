package com.example.gazproximite.ui.composants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.cleStock
import com.example.gazproximite.ui.theme.GrisInconnu
import com.example.gazproximite.ui.theme.GrisInconnuClair
import com.example.gazproximite.ui.theme.RougeRupture
import com.example.gazproximite.ui.theme.RougeRuptureClair
import com.example.gazproximite.ui.theme.VertDisponible
import com.example.gazproximite.ui.theme.VertDisponibleClair

// Couleur associée à chaque marque, pour la reconnaître d'un coup d'œil.
fun couleurMarque(marque: Marque): Color = when (marque) {
    Marque.PETROCI -> Color(0xFFF08A00)
    Marque.TOTALENERGIES -> Color(0xFFE2231A)
    Marque.SHELL -> Color(0xFFE8B400)
    Marque.ORYX -> Color(0xFF1565C0)
    Marque.CORLAY -> Color(0xFF2E7D32)
}

// Pastille ronde avec les initiales de la marque.
@Composable
fun BadgeMarque(
    marque: Marque,
    taille: Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(taille)
            .clip(CircleShape)
            .background(couleurMarque(marque)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = marque.libelle.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (taille.value * 0.36f).sp
        )
    }
}

// Pastille d'un format (B6, B12, B28) colorée selon sa disponibilité :
// vert = en stock, rouge = rupture, gris = non renseigné.
@Composable
fun PuceFormat(
    format: Format,
    disponible: Boolean?,
    modifier: Modifier = Modifier
) {
    val (couleurTexte, couleurFond) = when (disponible) {
        true -> VertDisponible to VertDisponibleClair
        false -> RougeRupture to RougeRuptureClair
        null -> GrisInconnu to GrisInconnuClair
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = couleurFond,
        modifier = modifier
    ) {
        Text(
            text = format.name,
            color = couleurTexte,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

// Disponibilité de toutes les bouteilles d'un dépôt :
// une ligne par marque, avec ses trois formats.
@Composable
fun DisponibiliteBouteilles(
    stock: Map<String, Boolean>
) {
    Column {
        Marque.entries.forEachIndexed { index, marque ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgeMarque(marque = marque, taille = 34.dp)

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = marque.libelle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Format.entries.forEach { format ->
                        PuceFormat(
                            format = format,
                            disponible = stock[cleStock(marque, format)],
                            modifier = Modifier.width(52.dp)
                        )
                    }
                }
            }

            if (index < Marque.entries.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

// Légende des couleurs du stock.
@Composable
fun LegendeStock() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ElementLegende(VertDisponible, "En stock")
        ElementLegende(RougeRupture, "Rupture")
        ElementLegende(GrisInconnu, "Non renseigné")
    }
}

@Composable
private fun ElementLegende(couleur: Color, texte: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(couleur)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = texte,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Nombre de bouteilles (marque + format) marquées en stock.
fun nombreEnStock(stock: Map<String, Boolean>): Int =
    stock.count { (_, disponible) -> disponible }
