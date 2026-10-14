package com.example.gazproximite.ui.screens.depots

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gazproximite.R
import com.example.gazproximite.ui.theme.YaDarkBlue
import com.example.gazproximite.ui.theme.YaBlue2
import com.example.gazproximite.ui.theme.AppBackground
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.ui.carte.CarteOsm
import com.example.gazproximite.ui.etat.EtatDepotsProches
import com.example.gazproximite.ui.screens.MessageAucunDepot
import com.example.gazproximite.ui.screens.formatDisponible
import com.example.gazproximite.ui.screens.home.CustomBottomNavBar
import com.example.gazproximite.ui.screens.home.DepotCardItem

// Options de tri de la liste des dépôts (clé -> libellé).
private val optionsTri = listOf(
    "distance" to "Distance (du plus proche)",
    "nom" to "Nom (de A à Z)",
    "ouverts" to "Dépôts ouverts d'abord"
)

/**
 * [EcranDepotsList] - Écran répertoriant tous les dépôts de gaz sous forme de liste ou de carte interactive.
 * Permet la recherche par mot-clé, le filtrage par type de bouteille (B6, B12, B28) et le tri.
 */
@Composable
fun EcranDepotsList(
    etat: EtatDepotsProches,
    onDepotClick: (DepotAvecDistance) -> Unit = {},
    onTabSelected: (Int) -> Unit = {}
) {
    // ==========================================
    // --- SECTION 1 : ÉTATS DE RECHERCHE ET FILTRES ---
    // ==========================================
    // Texte de recherche saisi par l'utilisateur
    var searchQuery by remember { mutableStateOf("") }

    // Filtre sélectionné ("Toutes", "B6", "B12", "B28")
    var selectedFilter by remember { mutableStateOf("Toutes") }

    // Bascule entre la vue Liste (false) et la vue Carte interactive (true)
    var isMapView by remember { mutableStateOf(false) }

    // Affichage de la feuille modale de tri et filtrage avancé
    var showFilterModal by remember { mutableStateOf(false) }

    // Critère de tri choisi ("distance" par défaut)
    var triChoisi by remember { mutableStateOf("distance") }
    val isSortApplied = triChoisi != "distance"

    // Filtrage dynamique selon le filtre de catégorie sélectionné et le texte de recherche
    val filteredDepots = remember(etat.depots, selectedFilter, searchQuery, triChoisi) {
        val filtres = etat.depots.filter { item ->
            val depot = item.depot
            val matchesFilter = when (selectedFilter) {
                "B6" -> formatDisponible(depot, Format.B6)
                "B12" -> formatDisponible(depot, Format.B12)
                "B28" -> formatDisponible(depot, Format.B28)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    depot.nom.contains(searchQuery, ignoreCase = true) ||
                    depot.commune.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }

        when (triChoisi) {
            "nom" -> filtres.sortedBy { it.depot.nom.lowercase() }
            "ouverts" -> filtres.sortedWith(
                compareByDescending<DepotAvecDistance> { it.depot.estOuvert }
                    .thenBy { it.distanceMetres }
            )
            else -> filtres.sortedBy { it.distanceMetres }
        }
    }

    // ==========================================
    // --- SECTION 2 : CONTENEUR RACINE ---
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize() // Occupe toute la surface de l'écran
            .background(AppBackground) // Applique la couleur de fond globale de l'application
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // Sécurité pour la barre d'état Android
        ) {
            // ==========================================
            // --- SECTION 3 : EN-TÊTE ET BASCULE VUE LISTE/CARTE ---
            // ==========================================
            Surface(
                color = AppBackground, // Fond identique à l'arrière-plan
                shadowElevation = 0.dp, // Aucune ombre
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_yagaz2),
                        contentDescription = "Logo officiel YaGaz",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.height(40.dp)
                    )

                    // Bouton rond basculant entre la vue Liste et la vue Carte
                    IconButton(
                        onClick = { isMapView = !isMapView }, // Bascule l'état
                        modifier = Modifier
                            .size(40.dp) // Zone de clic circulaire de 40.dp
                            .background(Color.White, CircleShape) // Fond blanc circulaire
                    ) {
                        Icon(
                            imageVector = if (isMapView) Icons.AutoMirrored.Filled.List else Icons.Filled.Map, // Icône liste ou carte
                            contentDescription = "Changer de mode d'affichage", // Accessibilité
                            tint = YaDarkBlue // Couleur bleue sombre YaGaz
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp) // Marge latérale uniforme de 20.dp
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // --- SECTION 4 : TITRE ET BARRE DE RECHERCHE ---
                // ==========================================
                Text(
                    text = "Liste des dépôts", // Titre principal
                    fontSize = 20.sp, // Taille de texte à 20.sp
                    fontWeight = FontWeight.Bold,
                    color = YaDarkBlue
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Champ de saisie interactif pour la recherche de dépôts ou bouteilles
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Rechercher une bouteille ou un vendeur...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Icône de recherche",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(24.dp), // Arrondi des coins à 24.dp
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YaDarkBlue, // Bordure bleue au focus
                        unfocusedBorderColor = Color(0xFFE0E0E0), // Bordure grise au repos
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth() // Largeur maximale
                        .height(50.dp), // Hauteur fixe de 50.dp
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // --- SECTION 5 : RANGÉE DE FILTRES ET BOUTON DE TRI ---
                // ==========================================
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Espacement de 8.dp entre puces
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item { FilterChipButton("Toutes", selectedFilter == "Toutes") { selectedFilter = "Toutes" } }
                    item { FilterChipButton("B6", selectedFilter == "B6") { selectedFilter = "B6" } }
                    item { FilterChipButton("B12", selectedFilter == "B12") { selectedFilter = "B12" } }
                    item { FilterChipButton("B28", selectedFilter == "B28") { selectedFilter = "B28" } }
                    item {
                        Surface(
                            onClick = { showFilterModal = true }, // Ouvre la feuille modale de tri
                            shape = RoundedCornerShape(20.dp), // Arrondi de 20.dp
                            color = if (isSortApplied) YaDarkBlue else Color.White, // Fond bleu si un tri est appliqué
                            shadowElevation = 2.dp,
                            modifier = Modifier.height(36.dp) // Hauteur de 36.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isSortApplied) "Trier ✓" else "Trier",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSortApplied) Color.White else YaDarkBlue
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Informations du nombre de dépôts trouvés
                Text(
                    text = if (etat.chargement) "Recherche en cours..." else "${filteredDepots.size} dépôt(s) trouvé(s)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // --- SECTION 6 : AFFICHAGE LISTE OU CARTE INTERACTIVE ---
                // ==========================================
                if (etat.chargement) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = YaDarkBlue)
                    }
                } else if (etat.depots.isEmpty()) {
                    MessageAucunDepot(etat = etat, modifier = Modifier.weight(1f))
                } else if (isMapView) {
                    // --- Affichage en Mode Carte : vraie carte OpenStreetMap ---
                    Box(
                        modifier = Modifier
                            .weight(1f) // Occupe le reste de l'espace vertical
                            .fillMaxWidth()
                            .padding(bottom = 90.dp) // Laisse la place à la barre de navigation
                            .clip(RoundedCornerShape(20.dp)) // Coins arrondis pour la carte
                            .background(Color.Black)
                    ) {
                        CarteOsm(
                            depots = filteredDepots,
                            positionClient = null,
                            afficherPositionClient = true,
                            depotSelectionne = null,
                            demandeRecentrage = 0,
                            onMarqueurClique = onDepotClick,
                            onCarteTouchee = {},
                            modifier = Modifier.fillMaxSize()
                        )

                        Text(
                            text = "Touchez un marqueur pour voir le dépôt  •  © OpenStreetMap © CARTO",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    // --- Affichage en Mode Liste Déroulante (LazyColumn) ---
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f) // Prend tout l'espace disponible
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        contentPadding = PaddingValues(bottom = 90.dp) // Espace pour ne pas être recouvert par la barre de nav
                    ) {
                        if (filteredDepots.isEmpty()) {
                            item {
                                Text(
                                    text = "Aucun dépôt ne correspond à votre recherche.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }

                        items(filteredDepots.size) { index ->
                            val depot = filteredDepots[index]
                            DepotCardItem(
                                depotAvecDistance = depot,
                                onClick = { onDepotClick(depot) }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // --- SECTION 7 : BARRE DE NAVIGATION FLOTTANTE ---
        // ==========================================
        CustomBottomNavBar(
            selectedTab = 1, // Onglet 'Carte / Liste' (Index 1)
            onTabSelected = onTabSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding() // Intègre la zone de sécurité Android
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp) // Marges extérieures pour la barre flottante
        )

        // ==========================================
        // --- SECTION 8 : MODALE DE FILTRES ET TRI ---
        // ==========================================
        if (showFilterModal) {
            DepotFilterSortModalSheet(
                triInitial = triChoisi,
                onDismiss = { showFilterModal = false },
                onApply = { tri ->
                    triChoisi = tri
                    showFilterModal = false
                }
            )
        }
    }
}

/**
 * Composant réutilisable pour la modale de filtre et de tri des dépôts.
 */
@Composable
fun DepotFilterSortModalSheet(
    triInitial: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var selectedSortOption by remember { mutableStateOf(triInitial) }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Ombrage semi-transparent du fond
                .clickable(onClick = onDismiss), // Fermeture lors du clic extérieur
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f) // Occupe 60% de la hauteur de l'écran
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) // Coins arrondis à 32.dp
                    .background(Color.White)
                    .clickable(enabled = false) {}
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // En-tête de la modale
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trier les dépôts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = YaDarkBlue
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F2F5))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Fermer la modale",
                                tint = YaDarkBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Options de tri
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        optionsTri.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSortOption = key }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E1E24)
                                )
                                RadioButton(
                                    selected = selectedSortOption == key,
                                    onClick = { selectedSortOption = key },
                                    colors = RadioButtonDefaults.colors(selectedColor = YaDarkBlue)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bouton de validation
                    Button(
                        onClick = { onApply(selectedSortOption) },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YaDarkBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp) // Hauteur de 50.dp
                    ) {
                        Text(
                            text = "Appliquer le tri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bouton de filtre de type puce (FilterChip).
 */
@Composable
fun FilterChipButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp), // Rayon de 20.dp
        color = if (isSelected) YaDarkBlue else Color.White, // Fond bleu sombre si sélectionné
        shadowElevation = 2.dp,
        modifier = Modifier.height(36.dp) // Hauteur de 36.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else YaDarkBlue
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun EcranDepotsListPreview() {
    EcranDepotsList(etat = com.example.gazproximite.ui.etat.EtatDepotsProches(chargement = false))
}
