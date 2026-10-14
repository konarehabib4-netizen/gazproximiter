package com.example.gazproximite.ui.ecrans

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gazproximite.R
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.ui.composants.BoutonsActionDepot
import com.example.gazproximite.ui.composants.DegradeFlamme
import com.example.gazproximite.ui.composants.DisponibiliteBouteilles
import com.example.gazproximite.ui.composants.LegendeStock
import com.example.gazproximite.ui.composants.LigneInfo
import com.example.gazproximite.ui.composants.PhotoDepot
import com.example.gazproximite.ui.composants.StatutOuverture
import com.example.gazproximite.ui.composants.TitreSection

// Détail d'un dépôt : horaires, stock complet et boutons d'action.
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
    ) {

        // =================================================
        // EN-TÊTE ILLUSTRÉ
        // =================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(DegradeFlamme)
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            // Cercle décoratif derrière l'illustration.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
            )

            Image(
                painter = painterResource(R.drawable.ill_bouteille_gaz),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .size(width = 100.dp, height = 125.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(0.62f)
            ) {
                IconButton(
                    onClick = onRetour,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = depot.nom.ifBlank { "Dépôt de gaz" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = depot.commune,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatutOuverture(estOuvert = depot.estOuvert)

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            // =================================================
            // PHOTO DU DÉPÔT
            // =================================================

            PhotoDepot(
                depot = depot,
                forme = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // =================================================
            // INFORMATIONS PRATIQUES
            // =================================================

            CarteSection {
                LigneInfo(
                    icone = Icons.Filled.NearMe,
                    libelle = "Distance",
                    valeur = formaterDistance(depotAvecDistance.distanceMetres)
                )

                LigneInfo(
                    icone = Icons.Filled.LocationCity,
                    libelle = "Commune",
                    valeur = depot.commune.ifBlank { "Non renseignée" }
                )

                if (depot.heureOuverture.isNotBlank() || depot.heureFermeture.isNotBlank()) {
                    LigneInfo(
                        icone = Icons.Filled.Schedule,
                        libelle = "Horaires",
                        valeur = "${depot.heureOuverture.ifBlank { "?" }} – ${depot.heureFermeture.ifBlank { "?" }}"
                    )
                }

                if (depot.telephone.isNotBlank()) {
                    LigneInfo(
                        icone = Icons.Filled.Phone,
                        libelle = "Téléphone",
                        valeur = depot.telephone
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =================================================
            // STOCK
            // =================================================

            Row(verticalAlignment = Alignment.CenterVertically) {
                TitreSection(titre = "Disponibilité des bouteilles")
            }

            Spacer(modifier = Modifier.height(8.dp))

            LegendeStock()

            Spacer(modifier = Modifier.height(12.dp))

            CarteSection {
                DisponibiliteBouteilles(stock = depot.stock)
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(16.dp))

            BoutonsActionDepot(depot = depot, context = context)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Carte blanche arrondie qui regroupe une section du détail.
@Composable
private fun CarteSection(
    contenu: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            content = contenu
        )
    }
}
