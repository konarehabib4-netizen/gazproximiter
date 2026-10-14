package com.example.gazproximite.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gazproximite.R
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.cleStock
import com.example.gazproximite.ui.carte.CarteOsm
import com.example.gazproximite.ui.etat.EtatDepotsProches
import com.example.gazproximite.ui.screens.ImageDepot
import com.example.gazproximite.ui.screens.MessageAucunDepot
import com.example.gazproximite.ui.screens.formatDisponible
import com.example.gazproximite.ui.screens.possedeFormat
import com.example.gazproximite.ui.theme.*

// Modèle de données pour la navigation
data class NavTabItem(
    val id: Int,
    val label: String,
    val iconRes: Int
)

/**
 * Filtres et tri choisis dans la modale "Filtres & Tri" de l'accueil.
 */
data class FiltresAccueil(
    val tri: String = "proche",
    val commune: String = TOUTES_COMMUNES,
    val formats: Set<Format> = emptySet(),
    val marques: Set<Marque> = emptySet(),
    val ouvertsSeulement: Boolean = false
)

const val TOUTES_COMMUNES = "Toutes"

/**
 * Applique les filtres de la modale à la liste des dépôts.
 */
fun appliquerFiltres(
    depots: List<DepotAvecDistance>,
    filtres: FiltresAccueil
): List<DepotAvecDistance> {
    val resultat = depots.filter { item ->
        val depot = item.depot

        val communeOk = filtres.commune == TOUTES_COMMUNES ||
                depot.commune.equals(filtres.commune, ignoreCase = true)

        // Le dépôt doit avoir en stock au moins une des bouteilles demandées.
        val marques = filtres.marques.ifEmpty { Marque.entries.toSet() }
        val formats = filtres.formats.ifEmpty { Format.entries.toSet() }
        val stockOk = (filtres.marques.isEmpty() && filtres.formats.isEmpty()) ||
                marques.any { marque ->
                    formats.any { format -> depot.stock[cleStock(marque, format)] == true }
                }

        val ouvertOk = !filtres.ouvertsSeulement || depot.estOuvert

        communeOk && stockOk && ouvertOk
    }

    return if (filtres.tri == "loin") {
        resultat.sortedByDescending { it.distanceMetres }
    } else {
        resultat.sortedBy { it.distanceMetres }
    }
}

/**
 * [EcranHome] - Écran d'accueil principal de l'application YaGaz.
 * Affiche l'en-tête, la recherche rapide, les puces de filtres et l'aperçu de la carte.
 */
@Composable
fun EcranHome(
    etat: EtatDepotsProches,
    onDepotClick: (DepotAvecDistance) -> Unit = {},
    onTabSelected: (Int) -> Unit = {}
) {
    // ==========================================
    // --- SECTION 1 : ÉTATS DE NAVIGATION ET RECHERCHE ---
    // ==========================================
    // Index de l'onglet sélectionné dans la barre de navigation
    var selectedTab by remember { mutableIntStateOf(0) }

    // Requête de recherche textuelle
    var searchQuery by remember { mutableStateOf("") }

    // Filtre rapide sélectionné ("B6", "B12", "B28", "Ouvert"), vide = aucun filtre
    var selectedFilter by remember { mutableStateOf("") }

    // Filtres avancés choisis dans la modale
    var filtres by remember { mutableStateOf(FiltresAccueil()) }

    // Contrôle de l'affichage de la modale complète de filtres et tri
    var showFilterModal by remember { mutableStateOf(false) }

    // --- ÉTATS ET GESTION DU GLISSEMENT INTELLIGENT DE LA FEUILLE ---
    var isSheetExpanded by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var lastDragAmount by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val maxDragPx = with(density) { 180.dp.toPx() }

    // Fraction d'extension calculée en temps réel selon le geste utilisateur
    val baseFraction = if (isSheetExpanded) 1f else 0f
    val liveFraction = (baseFraction - dragOffsetPx / maxDragPx).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = liveFraction,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 280f),
        label = "sheetLiveFraction"
    )

    // Hauteur animée de la carte MapPreview en temps réel
    val currentMapHeightDp = (180 * (1f - animatedFraction)).dp

    // --- DÉPÔTS RÉELS (FIRESTORE) ET FILTRAGE DYNAMIQUE DE LA FEUILLE ---
    val filteredDepots = remember(etat.depots, searchQuery, selectedFilter, filtres) {
        appliquerFiltres(etat.depots, filtres).filter { item ->
            val depot = item.depot
            val matchesSearch = searchQuery.isBlank() ||
                    depot.nom.contains(searchQuery, ignoreCase = true) ||
                    depot.commune.contains(searchQuery, ignoreCase = true) ||
                    Marque.entries.any { marque ->
                        marque.libelle.contains(searchQuery, ignoreCase = true) &&
                                Format.entries.any { depot.stock[cleStock(marque, it)] == true }
                    }

            val matchesChip = when (selectedFilter) {
                "B6" -> formatDisponible(depot, Format.B6)
                "B12" -> formatDisponible(depot, Format.B12)
                "B28" -> formatDisponible(depot, Format.B28)
                "Ouvert" -> depot.estOuvert
                else -> true
            }

            matchesSearch && matchesChip
        }
    }

    // Communes proposées dans la modale de filtres
    val communes = remember(etat.depots) {
        listOf(TOUTES_COMMUNES) + etat.depots.map { it.depot.commune }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // Un second appui sur la même puce retire le filtre
    val onChipSelected: (String) -> Unit = { filter ->
        if (filter == "Trier") {
            showFilterModal = true
        } else {
            selectedFilter = if (selectedFilter == filter) "" else filter
        }
    }

    // ==========================================
    // --- SECTION 3 : STRUCTURATION DE L'ÉCRAN ---
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxSize() // Occupe 100% de la surface d'affichage
            .background(AppBackground) // Fond global personnalisé YaGaz
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxHeight()
                .statusBarsPadding() // Marge de sécurité pour la barre de statut Android
        ) {
            // ==========================================
            // --- SECTION 4 : EN-TÊTE LOGO YAGAZ ---
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_yagaz2), // Logo principal YaGaz
                    contentDescription = "Logo officiel YaGaz",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.height(40.dp) // Hauteur fixée à 100.dp
                )
            }

            // ==========================================
            // --- SECTION 5 : RECHERCHE ET FILTRES AU-DESSUS DE LA CARTE (MODE INITIAL) ---
            // ==========================================
            AnimatedVisibility(
                visible = animatedFraction <= 0.45f,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    SearchBarSection(
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 2.dp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FilterChipsRowSection(
                        selectedFilter = selectedFilter,
                        onFilterSelected = onChipSelected,
                        horizontalPadding = 20.dp
                    )
                }
            }

            // ==========================================
            // --- SECTIONS 6 & 7 (FEUILLE ÉTIRABLE & APERÇU CARTE) ---
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // --- SECTION 6 : APERÇU DE LA CARTE DE GÉOLOCALISATION ---
                if (currentMapHeightDp > 0.dp) {
                    MapPreviewSection(
                        heightDp = currentMapHeightDp,
                        horizontalPadding = 10.dp,
                        depots = filteredDepots,
                        onDepotClick = onDepotClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- SECTION 7 : FEUILLE ÉTIRABLE AVEC RECHERCHE, FILTRES ET DÉPÔTS ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        colors = CardDefaults.cardColors(containerColor = Nato2), // Couleur Nato2 pour la carte
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // --- POIGNÉE DE GLISSEMENT EN TEMPS RÉEL ---
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 6.dp)
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                dragOffsetPx = 0f
                                                lastDragAmount = 0f
                                            },
                                            onDragEnd = {
                                                isSheetExpanded = when {
                                                    lastDragAmount < -6f -> true
                                                    lastDragAmount > 6f -> false
                                                    isSheetExpanded -> liveFraction > 0.65f
                                                    else -> liveFraction > 0.30f
                                                }
                                                dragOffsetPx = 0f
                                                lastDragAmount = 0f
                                            },
                                            onDragCancel = {
                                                dragOffsetPx = 0f
                                                lastDragAmount = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                lastDragAmount = dragAmount
                                                dragOffsetPx += dragAmount
                                            }
                                        )
                                    }
                                    .clickable { isSheetExpanded = !isSheetExpanded },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f))
                                )
                            }

                            // --- SECTION RECHERCHE ET FILTRES (VISIBLE UNIQUEMENT EN MODE ÉTIRÉ) ---
                            AnimatedVisibility(
                                visible = animatedFraction > 0.45f,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    SearchBarSection(
                                        searchQuery = searchQuery,
                                        onQueryChange = { searchQuery = it },
                                        modifier = Modifier.padding(
                                            horizontal = 20.dp,
                                            vertical = 2.dp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    FilterChipsRowSection(
                                        selectedFilter = selectedFilter,
                                        onFilterSelected = onChipSelected,
                                        horizontalPadding = 20.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // --- EN-TÊTE DÉPÔTS À PROXIMITÉ AVEC DÉCOMPTE DÉPOSITAIRES ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                dragOffsetPx = 0f
                                                lastDragAmount = 0f
                                            },
                                            onDragEnd = {
                                                isSheetExpanded = when {
                                                    lastDragAmount < -6f -> true
                                                    lastDragAmount > 6f -> false
                                                    isSheetExpanded -> liveFraction > 0.65f
                                                    else -> liveFraction > 0.30f
                                                }
                                                dragOffsetPx = 0f
                                                lastDragAmount = 0f
                                            },
                                            onDragCancel = {
                                                dragOffsetPx = 0f
                                                lastDragAmount = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                lastDragAmount = dragAmount
                                                dragOffsetPx += dragAmount
                                            }
                                        )
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Dépôts à proximité",
                                        fontFamily = NunitoBold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (etat.chargement) "Recherche en cours..." else "${filteredDepots.size} point(s) de vente trouvé(s)",
                                        fontFamily = ComfortaaBold,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { isSheetExpanded = !isSheetExpanded }
                                ) {
                                    Text(
                                        text = if (isSheetExpanded) "Réduire" else "Voir tout",
                                        fontFamily = ComfortaaBold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isSheetExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = if (isSheetExpanded) "Réduire la feuille" else "Étendre la feuille",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // --- LISTE DES DÉPÔTS FILTRÉS ---
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                if (etat.chargement) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 30.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = Color.White)
                                        }
                                    }
                                } else if (etat.depots.isEmpty()) {
                                    item {
                                        MessageAucunDepot(etat = etat, couleurTexte = Color.White)
                                    }
                                } else if (filteredDepots.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 30.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Aucun dépôt trouvé pour ce filtre",
                                                fontFamily = ComfortaaBold,
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else {
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
                }
            }
        }

        // ==========================================
        // --- SECTION 8 : BARRE DE NAVIGATION FLOTTANTE ---
        // ==========================================
        CustomBottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = {
                selectedTab = it
                onTabSelected(it)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
        )

        // ==========================================
        // --- SECTION 9 : FEUILLE MODALE DE FILTRES ET TRI ---
        // ==========================================
        if (showFilterModal) {
            FilterSortModalSheet(
                filtresInitiaux = filtres,
                communes = communes,
                onDismiss = { showFilterModal = false },
                onApply = { nouveauxFiltres ->
                    filtres = nouveauxFiltres
                    showFilterModal = false
                }
            )
        }
    }
}

/**
 * Modale complète de sélection de filtres et d'options de tri
 */
@Composable
fun FilterSortModalSheet(
    filtresInitiaux: FiltresAccueil,
    communes: List<String>,
    onDismiss: () -> Unit,
    onApply: (FiltresAccueil) -> Unit
) {
    var selectedSortOption by remember { mutableStateOf(filtresInitiaux.tri) }
    var selectedCommune by remember { mutableStateOf(filtresInitiaux.commune) }
    var selectedBottleTypes by remember { mutableStateOf(filtresInitiaux.formats) }
    var selectedBrands by remember { mutableStateOf(filtresInitiaux.marques) }
    var onlyOuvert by remember { mutableStateOf(filtresInitiaux.ouvertsSeulement) }

    var expandSortCategory by remember { mutableStateOf(true) }
    var expandLocationCategory by remember { mutableStateOf(true) }
    var expandBottleCategory by remember { mutableStateOf(true) }
    var expandBrandCategory by remember { mutableStateOf(true) }
    var expandStatusCategory by remember { mutableStateOf(true) }

    // Libellés affichés pour chaque format de bouteille
    val libellesFormats = mapOf(
        Format.B6 to "B6 (6 kg)",
        Format.B12 to "B12 (12.5 kg)",
        Format.B28 to "B28 (28 kg)"
    )

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
                    .fillMaxHeight(0.88f)
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
                            text = "Filtres & Tri",
                            fontFamily = NunitoBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF1E1E24)
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
                                contentDescription = "Fermer",
                                tint = Color(0xFF1E1E24),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ExpandableFilterSection(
                            title = "Trier par",
                            iconRes = R.drawable.ic_sort,
                            isExpanded = expandSortCategory,
                            onToggle = { expandSortCategory = !expandSortCategory }
                        ) {
                            val sortOptions = listOf(
                                "proche" to "Du plus proche au plus éloigné",
                                "loin" to "Du plus éloigné au plus proche"
                            )
                            sortOptions.forEach { (key, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSortOption = key }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = ComfortaaBold,
                                        fontSize = 14.sp,
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

                        ExpandableFilterSection(
                            title = "Localisation",
                            iconRes = R.drawable.ic_location,
                            isExpanded = expandLocationCategory,
                            onToggle = { expandLocationCategory = !expandLocationCategory }
                        ) {
                            SingleChoiceDropdownField(
                                label = "Commune",
                                selectedOption = selectedCommune,
                                options = communes,
                                onOptionSelected = { selectedCommune = it }
                            )
                        }

                        ExpandableFilterSection(
                            title = "Type de bouteille",
                            iconRes = R.drawable.ic_gas_cylinder,
                            isExpanded = expandBottleCategory,
                            onToggle = { expandBottleCategory = !expandBottleCategory }
                        ) {
                            MultiChoiceDropdownField(
                                label = "Sélectionner les bouteilles",
                                selectedOptions = selectedBottleTypes.map { libellesFormats.getValue(it) }.toSet(),
                                options = Format.entries.map { libellesFormats.getValue(it) },
                                onToggleOption = { option ->
                                    val format = libellesFormats.entries.first { it.value == option }.key
                                    selectedBottleTypes = if (format in selectedBottleTypes) {
                                        selectedBottleTypes - format
                                    } else {
                                        selectedBottleTypes + format
                                    }
                                }
                            )
                        }

                        ExpandableFilterSection(
                            title = "Marque de bouteille",
                            iconRes = R.drawable.ic_store,
                            isExpanded = expandBrandCategory,
                            onToggle = { expandBrandCategory = !expandBrandCategory }
                        ) {
                            MultiChoiceDropdownField(
                                label = "Sélectionner les marques",
                                selectedOptions = selectedBrands.map { it.libelle }.toSet(),
                                options = Marque.entries.map { it.libelle },
                                onToggleOption = { option ->
                                    val marque = Marque.entries.first { it.libelle == option }
                                    selectedBrands = if (marque in selectedBrands) {
                                        selectedBrands - marque
                                    } else {
                                        selectedBrands + marque
                                    }
                                }
                            )
                        }

                        ExpandableFilterSection(
                            title = "Disponibilité",
                            iconRes = R.drawable.ic_filter,
                            isExpanded = expandStatusCategory,
                            onToggle = { expandStatusCategory = !expandStatusCategory }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onlyOuvert = !onlyOuvert },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Dépôts ouverts uniquement",
                                    fontFamily = ComfortaaBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E1E24)
                                )
                                Switch(
                                    checked = onlyOuvert,
                                    onCheckedChange = { onlyOuvert = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = YaDarkBlue)
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
                                selectedSortOption = "proche"
                                selectedCommune = TOUTES_COMMUNES
                                selectedBottleTypes = emptySet()
                                selectedBrands = emptySet()
                                onlyOuvert = false
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Réinitialiser",
                                fontFamily = ComfortaaBold,
                                color = Color(0xFF1E1E24),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                onApply(
                                    FiltresAccueil(
                                        tri = selectedSortOption,
                                        commune = selectedCommune,
                                        formats = selectedBottleTypes,
                                        marques = selectedBrands,
                                        ouvertsSeulement = onlyOuvert
                                    )
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E1E24),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Valider les filtres",
                                fontFamily = ComfortaaBold,
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

@Composable
fun ExpandableFilterSection(
    title: String,
    iconRes: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F5F8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color(0xFF1E1E24),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        fontFamily = NunitoBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E1E24)
                    )
                }

                Icon(
                    painter = painterResource(id = if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SingleChoiceDropdownField(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = ComfortaaBold,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { isMenuExpanded = !isMenuExpanded },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedOption,
                        fontFamily = ComfortaaBold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E24)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier
                    .background(Color.White)
                    .width(260.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontFamily = ComfortaaBold,
                                fontSize = 14.sp,
                                fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == selectedOption) YaDarkBlue else Color(0xFF1E1E24)
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            isMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MultiChoiceDropdownField(
    label: String,
    selectedOptions: Set<String>,
    options: List<String>,
    onToggleOption: (String) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val summaryText = if (selectedOptions.isEmpty()) {
        "Aucune sélection"
    } else {
        selectedOptions.joinToString(", ")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = ComfortaaBold,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { isMenuExpanded = !isMenuExpanded },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summaryText,
                        fontFamily = ComfortaaBold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E24),
                        maxLines = 1
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier
                    .background(Color.White)
                    .width(260.dp)
            ) {
                options.forEach { option ->
                    val isChecked = selectedOptions.contains(option)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleOption(option) },
                                    colors = CheckboxDefaults.colors(checkedColor = YaDarkBlue)
                                )
                                Text(
                                    text = option,
                                    fontFamily = ComfortaaBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E1E24)
                                )
                            }
                        },
                        onClick = {
                            onToggleOption(option)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Composant de la barre de recherche
 */
@Composable
fun SearchBarSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Rechercher une bouteille ou un vendeur...",
                fontFamily = ComfortaaBold,
                fontSize = 13.sp,
                color = Color.Gray
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Rechercher",
                tint = Color(0xFF7A7A85),
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp), // Arrondi des coins à 24.dp
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp) // Hauteur de 52.dp
    )
}

/**
 * Rangée horizontale scrollable de filtres rapides
 */
@Composable
fun FilterChipsRowSection(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    horizontalPadding: Dp = 20.dp
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChipItem(
                text = "B6",
                isSelected = selectedFilter == "B6",
                hasCheckIcon = true,
                onClick = { onFilterSelected("B6") }
            )
        }
        item {
            FilterChipItem(
                text = "B12",
                isSelected = selectedFilter == "B12",
                hasCheckIcon = true,
                onClick = { onFilterSelected("B12") }
            )
        }
        item {
            FilterChipItem(
                text = "B28",
                isSelected = selectedFilter == "B28",
                hasCheckIcon = true,
                onClick = { onFilterSelected("B28") }
            )
        }
        item {
            FilterChipItem(
                text = "Ouvert",
                isSelected = selectedFilter == "Ouvert",
                hasCheckIcon = true,
                onClick = { onFilterSelected("Ouvert") }
            )
        }
        item {
            FilterChipItem(
                text = "Trier",
                isSelected = selectedFilter == "Trier",
                hasCheckIcon = false,
                leadingIconRes = R.drawable.ic_sort,
                onClick = { onFilterSelected("Trier") }
            )
        }
    }
}

/**
 * Puce individuelle de filtre
 */
@Composable
fun FilterChipItem(
    text: String,
    isSelected: Boolean,
    hasCheckIcon: Boolean = false,
    leadingIconRes: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp), // Arrondi de 20.dp
        color = if (isSelected) Color(0xFF1E1E24) else Color.White,
        modifier = Modifier.height(36.dp) // Hauteur de 36.dp
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
                    tint = if (isSelected) Color.White else Color(0xFF1E1E24),
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = text,
                fontFamily = ComfortaaBold,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else Color(0xFF1E1E24),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Section d'aperçu de la carte : vraie carte OpenStreetMap avec un marqueur par dépôt.
 * Toucher un marqueur ouvre le détail du dépôt.
 */
@Composable
fun MapPreviewSection(
    heightDp: Dp = 180.dp,
    width: Dp? = null,
    horizontalPadding: Dp = 20.dp,
    depots: List<DepotAvecDistance> = emptyList(),
    onDepotClick: (DepotAvecDistance) -> Unit = {}
) {
    if (heightDp > 20.dp) {
        Box(
            modifier = Modifier
                .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
                .height(heightDp)
                .padding(horizontal = if (width == null) horizontalPadding else 0.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(20.dp)) // Coins arrondis à 20.dp pour la carte
                .background(Color.Black)
        ) {
            CarteOsm(
                depots = depots,
                positionClient = null,
                afficherPositionClient = true,
                depotSelectionne = null,
                demandeRecentrage = 0,
                onMarqueurClique = onDepotClick,
                onCarteTouchee = {},
                modifier = Modifier.fillMaxSize()
            )

            // Crédit obligatoire des données OpenStreetMap
            Text(
                text = "© OpenStreetMap",
                fontFamily = ComfortaaBold,
                fontSize = 9.sp,
                color = Color.DarkGray,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

/**
 * Barre de navigation personnalisée en bas de l'écran avec découpe courbée dynamique.
 */
@Composable
fun CustomBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTabItem(0, "Accueil", R.drawable.ic_nav_home),
        NavTabItem(1, "Dépôt", R.drawable.ic_nav_map),
        NavTabItem(2, "Favoris", R.drawable.ic_nav_fav),
        NavTabItem(3, "Profil", R.drawable.ic_nav_profile)
    )

    val weights = List(tabs.size) { index ->
        val targetWeight = if (selectedTab == index) 1.8f else 0.8f
        animateFloatAsState(
            targetValue = targetWeight,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            label = "tabWeight_$index"
        ).value
    }

    val totalWeight = weights.sum()
    val activeCenterWeight = remember(weights, selectedTab) {
        var sum = 0f
        for (i in 0 until selectedTab) {
            sum += weights[i]
        }
        sum + (weights[selectedTab] / 2f)
    }
    val targetFraction = activeCenterWeight / totalWeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp) // Hauteur de 76.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val scale = w / 413f
            val barTop = 10f * scale
            val cX = w * targetFraction
            val cornerRadius = 15f * scale

            val barPath = Path().apply {
                moveTo(cornerRadius, barTop)

                val notchStart = cX - (22.73f * scale)
                val notchEnd = cX + (22.73f * scale)

                lineTo(notchStart, barTop)

                cubicTo(
                    cX - (12.24f * scale), barTop + (4.43f * scale),
                    cX - (4.6f * scale), barTop + (8.54f * scale),
                    cX, barTop + (8.54f * scale)
                )

                cubicTo(
                    cX + (4.6f * scale), barTop + (8.54f * scale),
                    cX + (12.24f * scale), barTop + (4.43f * scale),
                    notchEnd, barTop
                )

                lineTo(w - cornerRadius, barTop)
                quadraticTo(w, barTop, w, barTop + cornerRadius)

                lineTo(w, h - cornerRadius)
                quadraticTo(w, h, w - cornerRadius, h)

                lineTo(cornerRadius, h)
                quadraticTo(0f, h, 0f, h - cornerRadius)

                lineTo(0f, barTop + cornerRadius)
                quadraticTo(0f, barTop, cornerRadius, barTop)
                close()
            }

            drawPath(
                path = barPath,
                color = Color(0xFF1C2031) // Couleur bleu foncé de la barre
            )

            val circleRadius = 7.5f * scale
            val circleCenterY = 7.5f * scale
            drawCircle(
                color = Color(0xFF616E82),
                radius = circleRadius,
                center = Offset(cX, circleCenterY)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                val itemWeight = weights[index]

                Box(
                    modifier = Modifier
                        .weight(itemWeight)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = tab.label,
                                    fontFamily = ComfortaaBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.label,
                            tint = Color(0xFF8E99A8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Carte d'affichage d'un dépôt de gaz dans la liste déroulante
 */
@Composable
fun DepotCardItem(
    depotAvecDistance: DepotAvecDistance,
    onClick: () -> Unit = {}
) {
    val depot = depotAvecDistance.depot

    Card(
        shape = RoundedCornerShape(20.dp), // Rayon de 20.dp
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp) // Hauteur minimale flexible pour ne pas couper le contenu
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageDepot(
                depot = depot,
                modifier = Modifier
                    .size(72.dp) // Taille fixe de 72.dp x 72.dp
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = depot.nom.ifBlank { "Dépôt de gaz" },
                    fontFamily = NunitoBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E1E24)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (depot.estOuvert) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                                CircleShape
                            ) // Puce verte (ouvert) ou rouge (fermé)
                    )
                    Text(
                        text = "${if (depot.estOuvert) "Ouvert" else "Fermé"}  •  ${depot.commune}  •  ${formaterDistance(depotAvecDistance.distanceMetres)}",
                        fontFamily = ComfortaaBold,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (depot.estSimulation) {
                    Text(
                        text = "Dépôt de démonstration",
                        fontFamily = ComfortaaBold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Format.entries.forEach { format ->
                        if (possedeFormat(depot, format)) {
                            val dispo = formatDisponible(depot, format)
                            AvailabilityBadge(
                                text = "${format.name} • ${if (dispo) "Dispo" else "Fini"}",
                                isAvailable = dispo
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Puce d'indication de disponibilité d'une bouteille
 */
@Composable
fun AvailabilityBadge(text: String, isAvailable: Boolean) {
    val bgColor = if (isAvailable) Color(0xFFE2F4E9) else Color(0xFFFDE8E8)
    val textColor = if (isAvailable) Color(0xFF0A6B3D) else Color(0xFFC62828)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = NunitoBold,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            maxLines = 1
        )
    }
}
