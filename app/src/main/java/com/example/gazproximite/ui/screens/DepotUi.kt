package com.example.gazproximite.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.gazproximite.R
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.cleStock
import com.example.gazproximite.ui.etat.EtatDepotsProches
import com.example.gazproximite.ui.theme.ComfortaaBold
import com.example.gazproximite.ui.theme.YaDarkBlue

// Images de l'application utilisées quand un dépôt n'a pas de photo.
private val imagesParDefaut = listOf(
    R.drawable.image5,
    R.drawable.image1,
    R.drawable.image3,
    R.drawable.image6
)

// Choisit toujours la même image pour un même dépôt.
fun imageParDefaut(depot: Depot): Int =
    imagesParDefaut[Math.floorMod(depot.id.hashCode(), imagesParDefaut.size)]

// Photo du dépôt (champ "photoUrl" dans Firestore),
// ou une image de l'application si le dépôt n'en a pas.
@Composable
fun ImageDepot(
    depot: Depot,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val imageLocale = painterResource(id = imageParDefaut(depot))

    if (depot.photoUrl.isBlank()) {
        Image(
            painter = imageLocale,
            contentDescription = depot.nom,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = depot.photoUrl,
            contentDescription = depot.nom,
            placeholder = imageLocale,
            error = imageLocale,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

// Le dépôt vend-il ce format (au moins une marque renseignée) ?
fun possedeFormat(depot: Depot, format: Format): Boolean =
    Marque.entries.any { marque -> depot.stock.containsKey(cleStock(marque, format)) }

// Ce format est-il en stock pour au moins une marque ?
fun formatDisponible(depot: Depot, format: Format): Boolean =
    Marque.entries.any { marque -> depot.stock[cleStock(marque, format)] == true }

// Message affiché quand aucun dépôt n'a pu être chargé,
// avec les boutons pour corriger le problème.
@Composable
fun MessageAucunDepot(
    etat: EtatDepotsProches,
    couleurTexte: Color = YaDarkBlue,
    modifier: Modifier = Modifier
) {
    // Ouvre les réglages de localisation, puis recharge au retour.
    val ouvrirReglages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        etat.recharger()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = etat.messageErreur.ifBlank { "Aucun dépôt trouvé." },
            fontFamily = ComfortaaBold,
            fontSize = 13.sp,
            color = couleurTexte,
            textAlign = TextAlign.Center
        )

        if (etat.localisationDesactivee) {
            Button(
                onClick = {
                    ouvrirReglages.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("Activer la localisation", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = etat.recharger,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text("Réessayer", color = couleurTexte, fontWeight = FontWeight.Bold)
        }
    }
}
