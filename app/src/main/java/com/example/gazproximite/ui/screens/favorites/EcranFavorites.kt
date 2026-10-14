package com.example.gazproximite.ui.screens.favorites

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gazproximite.R
import com.example.gazproximite.ui.theme.YaDarkBlue
import com.example.gazproximite.ui.theme.AppBackground
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.ui.etat.EtatDepotsProches
import com.example.gazproximite.ui.screens.home.CustomBottomNavBar
import com.example.gazproximite.ui.screens.home.DepotCardItem

/**
 * [EcranFavorites] - Écran affichant la liste des dépôts de gaz sauvegardés en favoris par l'utilisateur.
 * Intègre un en-tête dédié, le décompte des favoris et la barre de navigation personnalisée.
 */
@Composable
fun EcranFavorites(
    etat: EtatDepotsProches,
    favoris: Set<String>,
    onDepotClick: (DepotAvecDistance) -> Unit = {},
    onTabSelected: (Int) -> Unit = {}
) {
    // ==========================================
    // --- SECTION 1 : DÉPÔTS MARQUÉS EN FAVORIS (cœur sur l'écran de détail) ---
    // ==========================================
    val favoriteDepots = remember(etat.depots, favoris) {
        etat.depots.filter { it.depot.id in favoris }
    }

    // ==========================================
    // --- SECTION 2 : CONTENEUR PRINCIPAL DE L'ÉCRAN ---
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize() // Occupe 100% de la surface d'affichage du téléphone
            .background(AppBackground) // Applique la couleur de fond globale de l'application YaGaz
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Remplit toute la hauteur disponible
                .statusBarsPadding() // Intègre le padding pour ne pas être masqué par le haut de l'écran Android
        ) {
            // ==========================================
            // --- SECTION 3 : EN-TÊTE LOGO YAGAZ ---
            // ==========================================
            Surface(
                color = AppBackground, // Fond identique au reste pour un dégradé seamless sans couture
                shadowElevation = 0.dp, // Aucune ombre portée sur l'en-tête
                modifier = Modifier
                    .fillMaxWidth() // S'étend sur toute la largeur
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_yagaz2),
                        contentDescription = "Logo officiel YaGaz",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.height(40.dp)
                    )
                }
            }

            // ==========================================
            // --- SECTION 4 : CONTENU & LISTE DES FAVORIS ---
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize() // S'étend sur toute la zone sous l'en-tête
                    .padding(horizontal = 20.dp) // Marge latérale uniforme de 20.dp
            ) {
                Spacer(modifier = Modifier.height(12.dp)) // Espacement supérieur de 12.dp

                // --- Sous-section 4.1 : Titre de l'écran ---
                Text(
                    text = "Mes dépôts favoris", // Titre explicite
                    fontSize = 20.sp, // Taille de police lisible à 20.sp
                    fontWeight = FontWeight.Bold, // Graisse forte pour la hiérarchie visuelle
                    color = YaDarkBlue // Couleur bleu sombre signature YaGaz
                )

                Spacer(modifier = Modifier.height(12.dp)) // Espacement de 12.dp

                // --- Sous-section 4.2 : Décompte du nombre de résultats ---
                Text(
                    text = when {
                        etat.chargement -> "Chargement..."
                        favoriteDepots.isEmpty() -> "Aucun favori pour l'instant. Touchez le cœur sur la page d'un dépôt pour l'ajouter ici."
                        else -> "${favoriteDepots.size} dépôt(s) trouvé(s)"
                    },
                    fontSize = 13.sp, // Taille de texte secondaire
                    fontWeight = FontWeight.SemiBold, // Graisse intermédiaire
                    color = Color(0xFF757575) // Gris neutre pour une donnée d'information
                )

                Spacer(modifier = Modifier.height(10.dp)) // Espacement de 10.dp avant la liste

                // --- Sous-section 4.3 : Liste déroulante des dépôts favoris (LazyColumn) ---
                LazyColumn(
                    modifier = Modifier
                        .weight(1f) // Prend tout l'espace vertical disponible au-dessus de la bottom nav
                        .fillMaxWidth(), // Occupe toute la largeur
                    verticalArrangement = Arrangement.spacedBy(10.dp), // Espacement régulier de 10.dp entre chaque carte
                    contentPadding = PaddingValues(bottom = 90.dp) // Padding en bas de 90.dp pour que la nav bar ne masque pas le dernier élément
                ) {
                    items(favoriteDepots.size) { index ->
                        val depot = favoriteDepots[index]
                        DepotCardItem(
                            depotAvecDistance = depot,
                            onClick = { onDepotClick(depot) } // Navigation vers l'écran de détails du dépôt
                        )
                    }
                }
            }
        }

        // ==========================================
        // --- SECTION 5 : BARRE DE NAVIGATION FLOTTANTE ---
        // ==========================================
        CustomBottomNavBar(
            selectedTab = 2, // Onglet 'Favoris' sélectionné par défaut (Index 2)
            onTabSelected = onTabSelected, // Callback de changement d'onglet vers le contrôleur principal
            modifier = Modifier
                .align(Alignment.BottomCenter) // Positionné en bas au centre de la Box
                .navigationBarsPadding() // Sécurité pour éviter la barre d'onglets système d'Android
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp) // Marges extérieures pour l'effet flottant
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun EcranFavoritesPreview() {
    EcranFavorites(etat = EtatDepotsProches(chargement = false), favoris = emptySet())
}
