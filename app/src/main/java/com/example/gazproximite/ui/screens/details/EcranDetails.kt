package com.example.gazproximite.ui.screens.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gazproximite.R
import com.example.gazproximite.ui.theme.YaDarkBlue
import com.example.gazproximite.ui.theme.AppBackground
import androidx.compose.ui.platform.LocalContext
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.cleStock
import com.example.gazproximite.ui.appelerDepot
import com.example.gazproximite.ui.ouvrirItineraire
import com.example.gazproximite.ui.ouvrirWhatsApp
import com.example.gazproximite.ui.partagerDepot
import com.example.gazproximite.ui.screens.ImageDepot
import com.example.gazproximite.ui.screens.home.CustomBottomNavBar

// ==========================================
// --- DIMENSIONS & PROPRIÉTÉS LOCALES ---
// ==========================================
// Dimension 1 : Hauteur de la bannière image en haut de l'écran de détails (220.dp)
private val DetailsBannerHeight: Dp = 220.dp

// Dimension 2 : Padding horizontal pour la barre d'action supérieure (16.dp)
private val DetailsHeaderPaddingHorizontal: Dp = 16.dp

// Dimension 3 : Taille des boutons d'action circulaires (retour, favori, partage) (40.dp)
private val DetailsActionButtonSize: Dp = 40.dp

// Dimension 4 : Taille de l'avatar circulaire du dépôt (82.dp)
private val DetailsAvatarSize: Dp = 82.dp

// Dimension 5 : Hauteur standard des boutons d'action d'appel et WhatsApp (50.dp)
private val DetailsButtonHeight: Dp = 50.dp

// Énumération des types et couleurs de bouteilles de gaz disponibles
enum class BottleType {
    BLUE_PETROCI,
    SILVER_PRIMAGAZ,
    GREEN_PETROCI
}

// Noms des marques et des formats utilisés par les filtres
private val nomsMarques = Marque.entries.map { it.libelle }
private val nomsFormats = Format.entries.map { it.name }

// Couleur de bouteille dessinée pour chaque marque
private fun typeBouteille(marque: Marque): BottleType = when (marque) {
    Marque.PETROCI, Marque.CORLAY -> BottleType.GREEN_PETROCI
    Marque.TOTALENERGIES, Marque.ORYX -> BottleType.BLUE_PETROCI
    Marque.SHELL -> BottleType.SILVER_PRIMAGAZ
}

// Modèle de données pour une bouteille de gaz
data class BottleItemData(
    val name: String,
    val price: String,
    val isAvailable: Boolean,
    val type: BottleType,
    val brand: String
)

/**
 * [EcranDetails] - Écran détaillé d'un point de vente / dépôt de gaz.
 * Affiche la couverture du point de vente, ses coordonnées, les bouteilles en stock, et les boutons d'appel/contact direct.
 */
@Composable
fun EcranDetails(
    depotAvecDistance: DepotAvecDistance,
    estFavori: Boolean = false,
    onFavoriClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val depot = depotAvecDistance.depot

    // ==========================================
    // --- SECTION 1 : ÉTATS ET DONNÉES LOCALE ---
    // ==========================================
    // Ensemble des filtres sélectionnés (Marques & Tailles de bouteilles)
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }

    // Affichage de la modale de filtre et de tri
    var showFilterModal by remember { mutableStateOf(false) }

    // Onglet sélectionné pour la barre de navigation
    var selectedNavTab by remember { mutableIntStateOf(0) }

    // Bouteilles du dépôt, construites à partir de son stock Firestore
    val allBottles = remember(depot) {
        Marque.entries.flatMap { marque ->
            Format.entries.mapNotNull { format ->
                val disponible = depot.stock[cleStock(marque, format)] ?: return@mapNotNull null
                BottleItemData(
                    name = "${format.name} . ${marque.libelle}",
                    price = "Prix au dépôt",
                    isAvailable = disponible,
                    type = typeBouteille(marque),
                    brand = marque.libelle
                )
            }
        }.sortedByDescending { it.isAvailable }
    }

    // Filtrage cumulatif multi-critères
    val filteredBottles = remember(selectedFilters) {
        if (selectedFilters.isEmpty()) {
            allBottles
        } else {
            val selectedBrands = selectedFilters.filter { it in nomsMarques }
            val selectedSizes = selectedFilters.filter { it in nomsFormats }

            allBottles.filter { bottle ->
                val matchesBrand = selectedBrands.isEmpty() || selectedBrands.contains(bottle.brand)
                val matchesSize = selectedSizes.isEmpty() || selectedSizes.any { size -> bottle.name.contains(size) }
                matchesBrand && matchesSize
            }
        }
    }

    val verticalScrollState = rememberScrollState()

    // ==========================================
    // --- SECTION 2 : CONTENEUR PRINCIPAL ---
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize() // Occupe toute la surface de l'écran
            .background(AppBackground) // Fond global personnalisé
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState) // Rend le contenu défilable
                .padding(bottom = 160.dp) // Padding inférieur pour éviter que les boutons d'action et la nav bar ne recouvrent le contenu
        ) {
            // ==========================================
            // --- SECTION 3 : BANNIÈRE D'EN-TÊTE & BOUTONS SURVOLANTS ---
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DetailsBannerHeight) // Hauteur fixée à 220.dp
            ) {
                // Image de couverture du dépôt
                ImageDepot(
                    depot = depot,
                    contentScale = ContentScale.Crop, // Recadrage élégant
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)) // Coins inférieurs arrondis à 24.dp
                )

                // Rangée des boutons d'action (Retour, Favori, Partager)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding() // Sécurité barre de statut
                        .padding(horizontal = DetailsHeaderPaddingHorizontal, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton Retour
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(DetailsActionButtonSize) // Taille de 40.dp
                            .background(Color(0x66000000), CircleShape) // Fond noir translucide
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retourner à l'écran précédent",
                            tint = Color.White
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bouton Favori
                        IconButton(
                            onClick = onFavoriClick,
                            modifier = Modifier
                                .size(DetailsActionButtonSize) // 40.dp
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (estFavori) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Ajouter ou retirer des favoris",
                                tint = if (estFavori) Color.Red else Color.White
                            )
                        }

                        // Bouton Partage
                        IconButton(
                            onClick = { partagerDepot(context, depot) },
                            modifier = Modifier
                                .size(DetailsActionButtonSize) // 40.dp
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Partager ce dépôt",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Avatar du magasin chevauchant la bannière
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 20.dp, y = 38.dp) // Chevauchement contrôlé vers le bas de 38.dp
                ) {
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(3.5.dp, Color.White), // Contour blanc épais
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(DetailsAvatarSize) // Taille de 82.dp
                    ) {
                        ImageDepot(
                            depot = depot,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Badge de statut "Ouvert"
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, if (depot.estOuvert) Color(0xFFA5D6A7) else Color(0xFFEF9A9A)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-20).dp, y = (-12).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = if (depot.estOuvert) Color(0xFF0A6B3D) else Color(0xFFC62828))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (depot.estOuvert) "Ouvert" else "Fermé",
                            color = if (depot.estOuvert) Color(0xFF0A6B3D) else Color(0xFFC62828),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ==========================================
            // --- SECTION 4 : INFORMATIONS PRINCIPALES DU DÉPÔT ---
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = depot.nom.ifBlank { "Dépôt de gaz" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = YaDarkBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified, // Badge vérifié
                        contentDescription = "Vendeur certifié YaGaz",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Horaires d'ouverture
                if (depot.heureOuverture.isNotBlank() || depot.heureFermeture.isNotBlank()) {
                    Text(
                        text = "Horaires : ${depot.heureOuverture.ifBlank { "?" }} – ${depot.heureFermeture.ifBlank { "?" }}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = YaDarkBlue
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Adresse et géolocalisation
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${depot.commune.ifBlank { "Commune inconnue" }}, Abidjan  •  ${formaterDistance(depotAvecDistance.distanceMetres)}",
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // --- SECTION 5 : INFORMATIONS D'ÉCHANGE DE BOUTEILLE ---
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEAF6EE), // Fond vert très clair réconfortant
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { ouvrirItineraire(context, depot) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = YaDarkBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Itinéraire",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = YaDarkBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (depot.estSimulation) {
                            "Dépôt de démonstration : ses informations sont fictives. Touchez ici pour voir sa position sur la carte."
                        } else {
                            "Touchez ici pour ouvrir l'itinéraire vers ce dépôt dans votre application de cartes."
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF1B5E20),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // --- SECTION 6 : INVENTAIRE DES BOUTEILLES & PRIX ---
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bouteilles et disponibilité",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = YaDarkBlue
                )
                Text(
                    text = "Voir tout",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2770A4),
                    modifier = Modifier.clickable { selectedFilters = emptySet() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Puces de filtrage interactives
            ProfileFilterChipsRowSection(
                selectedFilters = selectedFilters,
                onToggleFilter = { filter ->
                    selectedFilters = if (filter == "Toutes") {
                        emptySet()
                    } else if (selectedFilters.contains(filter)) {
                        selectedFilters - filter
                    } else {
                        selectedFilters + filter
                    }
                },
                onOpenModal = { showFilterModal = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Grille d'affichage des cartes de bouteilles
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFCDD2D7),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp) // Hauteur maximale scrollable
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredBottles.isEmpty()) {
                            Text(
                                text = "Aucune bouteille renseignée pour ce filtre.",
                                fontSize = 13.sp,
                                color = YaDarkBlue
                            )
                        }

                        filteredBottles.chunked(2).forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { bottle ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        BottleCard(
                                            bottle = bottle,
                                            onCommander = { appelerDepot(context, depot.telephone) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // --- SECTION 7 : BOUTONS D'ACTION DIRECTE (APPELER / WHATSAPP) ---
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(AppBackground)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bouton Appeler
                Button(
                    onClick = { appelerDepot(context, depot.telephone) },
                    enabled = depot.telephone.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = YaDarkBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(DetailsButtonHeight) // Hauteur fixe de 50.dp
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Appeler le vendeur",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Appeler",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Bouton WhatsApp
                Button(
                    onClick = { ouvrirWhatsApp(context, depot.telephone) },
                    enabled = depot.telephone.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F8B5F)), // Vert officiel WhatsApp
                    modifier = Modifier
                        .weight(1f)
                        .height(DetailsButtonHeight) // Hauteur fixe de 50.dp
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Discuter sur WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WhatsApp",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barre de navigation inférieure
            CustomBottomNavBar(
                selectedTab = selectedNavTab,
                onTabSelected = {
                    selectedNavTab = it
                    onTabSelected(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }

        // ==========================================
        // --- SECTION 8 : MODALE DE FILTRE ET DE TRI ---
        // ==========================================
        if (showFilterModal) {
            ProfileFilterSortModalSheet(
                initialSelectedFilters = selectedFilters,
                onDismiss = { showFilterModal = false },
                onApply = { newFilters ->
                    selectedFilters = newFilters
                    showFilterModal = false
                }
            )
        }
    }
}

/**
 * Rangée scrollable des puces de filtrage
 */
@Composable
fun ProfileFilterChipsRowSection(
    selectedFilters: Set<String>,
    onToggleFilter: (String) -> Unit,
    onOpenModal: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filters = listOf("Toutes") + nomsMarques + nomsFormats + "Trier"
        filters.forEach { filterText ->
            val isTrier = filterText == "Trier"
            val isToutes = filterText == "Toutes"
            val isSelected = if (isToutes) selectedFilters.isEmpty() else selectedFilters.contains(filterText)

            ProfileFilterChipItem(
                text = filterText,
                isSelected = isSelected,
                hasCheckIcon = !isTrier && !isToutes,
                leadingIconRes = if (isTrier) R.drawable.ic_sort else null,
                onClick = {
                    if (isTrier) {
                        onOpenModal()
                    } else if (isToutes) {
                        onToggleFilter("Toutes")
                    } else {
                        onToggleFilter(filterText)
                    }
                }
            )
        }
    }
}

/**
 * Puce de filtre individuelle
 */
@Composable
fun ProfileFilterChipItem(
    text: String,
    isSelected: Boolean,
    hasCheckIcon: Boolean = false,
    leadingIconRes: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) YaDarkBlue else Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected && hasCheckIcon) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            } else if (leadingIconRes != null) {
                Icon(
                    painter = painterResource(id = leadingIconRes),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else YaDarkBlue,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else YaDarkBlue
            )
        }
    }
}

/**
 * Modale de filtre et de tri pour l'écran de détails
 */
@Composable
fun ProfileFilterSortModalSheet(
    initialSelectedFilters: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var selectedBrands by remember {
        mutableStateOf(
            initialSelectedFilters.filter { it in nomsMarques }.toSet()
        )
    }
    var selectedSizes by remember {
        mutableStateOf(
            initialSelectedFilters.filter { it in nomsFormats }.toSet()
        )
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .clickable(enabled = false) {}
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtrer les bouteilles",
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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 2: Type de bouteille
                        Text(
                            text = "Type de bouteille",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = YaDarkBlue
                        )
                        val sizes = nomsFormats
                        sizes.forEach { size ->
                            val isChecked = selectedSizes.contains(size)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSizes = if (isChecked) selectedSizes - size else selectedSizes + size
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = size,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E1E24)
                                )
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedSizes = if (checked) selectedSizes + size else selectedSizes - size
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = YaDarkBlue)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE0E0E0))

                        // Section 3: Marque de bouteille
                        Text(
                            text = "Marque de bouteille",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = YaDarkBlue
                        )
                        val brands = nomsMarques
                        brands.forEach { brand ->
                            val isChecked = selectedBrands.contains(brand)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBrands = if (isChecked) selectedBrands - brand else selectedBrands + brand
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = brand,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E1E24)
                                )
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedBrands = if (checked) selectedBrands + brand else selectedBrands - brand
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = YaDarkBlue)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedBrands = emptySet()
                                selectedSizes = emptySet()
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Réinitialiser",
                                color = YaDarkBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { onApply(selectedBrands + selectedSizes) },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YaDarkBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Appliquer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Carte produit individuelle représentant une bouteille de gaz
 */
@Composable
fun BottleCard(bottle: BottleItemData, onCommander: () -> Unit = {}, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        modifier = modifier
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = if (bottle.isAvailable) Color(0xFF0A6B3D) else Color(0xFFC62828))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (bottle.isAvailable) "Disponible" else "Rupture",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (bottle.isAvailable) Color(0xFF0A6B3D) else Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            GasBottleVector(
                type = bottle.type,
                modifier = Modifier
                    .height(65.dp)
                    .width(42.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = bottle.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = YaDarkBlue,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = bottle.price,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = YaDarkBlue,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onCommander,
                enabled = bottle.isAvailable,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F8B5F)),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                Text(
                    text = "Commander",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Rendu vectoriel personnalisé des bouteilles de gaz en fonction de la marque et du type
 */
@Composable
fun GasBottleVector(type: BottleType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (type) {
            BottleType.BLUE_PETROCI -> {
                val primaryColor = Color(0xFF1976D2)
                val darkColor = Color(0xFF0D47A1)
                val lightColor = Color(0xFF42A5F5)

                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(w * 0.38f, 0f),
                    size = Size(w * 0.24f, h * 0.12f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                drawRoundRect(
                    color = Color(0xFF263238),
                    topLeft = Offset(w * 0.25f, h * 0.08f),
                    size = Size(w * 0.5f, h * 0.14f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 3.5f)
                )

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(darkColor, primaryColor, lightColor, primaryColor)
                    ),
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.28f, w * 0.28f)
                )

                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.18f, h * 0.48f),
                    size = Size(w * 0.64f, h * 0.14f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
            BottleType.SILVER_PRIMAGAZ -> {
                val primaryColor = Color(0xFF78909C)
                val darkColor = Color(0xFF455A64)
                val lightColor = Color(0xFFCFD8DC)

                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(w * 0.38f, 0f),
                    size = Size(w * 0.24f, h * 0.12f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                drawRoundRect(
                    color = Color(0xFF263238),
                    topLeft = Offset(w * 0.25f, h * 0.08f),
                    size = Size(w * 0.5f, h * 0.14f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 3.5f)
                )

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(darkColor, primaryColor, lightColor, primaryColor)
                    ),
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.28f, w * 0.28f)
                )

                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.18f, h * 0.48f),
                    size = Size(w * 0.64f, h * 0.14f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
            BottleType.GREEN_PETROCI -> {
                val primaryColor = Color(0xFF2E7D32)
                val darkColor = Color(0xFF1B5E20)
                val lightColor = Color(0xFF66BB6A)

                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(w * 0.38f, 0f),
                    size = Size(w * 0.24f, h * 0.12f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                drawRoundRect(
                    color = Color(0xFF263238),
                    topLeft = Offset(w * 0.25f, h * 0.08f),
                    size = Size(w * 0.5f, h * 0.14f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 3.5f)
                )

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(darkColor, primaryColor, lightColor, primaryColor)
                    ),
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.28f, w * 0.28f)
                )

                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.18f, h * 0.48f),
                    size = Size(w * 0.64f, h * 0.14f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun EcranDetailsPreview() {
    EcranDetails(
        depotAvecDistance = DepotAvecDistance(
            depot = com.example.gazproximite.data.model.depotsSimulation.first(),
            distanceMetres = 2400f
        )
    )
}
