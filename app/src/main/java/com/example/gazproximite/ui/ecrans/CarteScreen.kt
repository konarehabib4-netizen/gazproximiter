package com.example.gazproximite.ui.ecrans

import android.Manifest
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.location.LocationRepository
import com.example.gazproximite.data.location.calculerDistanceMetres
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.model.depotsSimulation
import com.example.gazproximite.data.repository.DepotRepository
import com.example.gazproximite.ui.composants.LogoGaz
import com.example.gazproximite.ui.composants.PhotoDepot
import com.example.gazproximite.ui.composants.StatutOuverture
import com.example.gazproximite.ui.composants.nombreEnStock
import com.example.gazproximite.ui.composants.ouvrirItineraire
import com.example.gazproximite.ui.theme.GrisInconnu
import com.example.gazproximite.ui.theme.Flamme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CarteScreen"

// Onglet "Carte" : tous les dépôts sur une carte OpenStreetMap.
// Le bouton "Simulation" ajoute des dépôts fictifs pour tester l'affichage.
@Composable
fun CarteScreen(
    context: Context,
    onDepotClique: (DepotAvecDistance) -> Unit
) {
    val locationRepository = remember { LocationRepository(context) }

    // Dépôts enregistrés dans Firestore.
    var depotsFirestore by remember { mutableStateOf<List<Depot>>(emptyList()) }

    // Position du client (null tant qu'elle est inconnue).
    var positionClient by remember { mutableStateOf<Location?>(null) }

    var chargement by remember { mutableStateOf(true) }
    var localisationAutorisee by remember {
        mutableStateOf(locationRepository.localisationAutorisee())
    }

    // Mode simulation : ajoute les dépôts fictifs sur la carte.
    var simulation by remember { mutableStateOf(false) }

    // Dépôt touché sur la carte (affiché dans la fiche du bas).
    var depotSelectionne by remember { mutableStateOf<DepotAvecDistance?>(null) }

    // Chaque incrément demande à la carte de se recentrer sur le client.
    var demandeRecentrage by remember { mutableIntStateOf(0) }

    val demandePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        localisationAutorisee =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Charge les dépôts Firestore (même sans position, pour la carte).
    LaunchedEffect(Unit) {
        if (!localisationAutorisee) {
            demandePermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        try {
            depotsFirestore = withContext(Dispatchers.IO) {
                DepotRepository().recupererDepots()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Échec du chargement des dépôts", e)
        }
        chargement = false
    }

    // Récupère la position dès que la permission est accordée.
    LaunchedEffect(localisationAutorisee) {
        if (localisationAutorisee && locationRepository.localisationActivee()) {
            positionClient = withContext(Dispatchers.IO) {
                locationRepository.recupererPosition()
            }
        }
    }

    // Dépôts affichés : ceux de Firestore, plus les fictifs en simulation.
    // La distance vaut NaN si la position du client est inconnue.
    val depotsAffiches = (if (simulation) depotsFirestore + depotsSimulation else depotsFirestore)
        .filter { it.position != null }
        .map { depot ->
            val position = positionClient
            val positionDepot = depot.position!!

            DepotAvecDistance(
                depot = depot,
                distanceMetres = if (position == null) {
                    Float.NaN
                } else {
                    calculerDistanceMetres(
                        position.latitude, position.longitude,
                        positionDepot.latitude, positionDepot.longitude
                    )
                }
            )
        }

    Box(modifier = Modifier.fillMaxSize()) {

        // La carte occupe tout l'écran.
        CarteOsm(
            depots = depotsAffiches,
            positionClient = positionClient,
            afficherPositionClient = localisationAutorisee,
            depotSelectionne = depotSelectionne?.depot,
            demandeRecentrage = demandeRecentrage,
            onMarqueurClique = { depotSelectionne = it },
            onCarteTouchee = { depotSelectionne = null },
            modifier = Modifier.fillMaxSize()
        )

        // =================================================
        // BANDEAU DU HAUT
        // =================================================

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LogoGaz(taille = 38.dp)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Carte des dépôts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = when {
                                    chargement -> "Chargement..."
                                    depotsAffiches.isEmpty() ->
                                        "Aucun dépôt localisé. Active la simulation."
                                    else -> "${depotsAffiches.size} dépôt(s) sur la carte"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Switch(
                                checked = simulation,
                                onCheckedChange = {
                                    simulation = it
                                    depotSelectionne = null
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF1565C0)
                                )
                            )

                            Text(
                                text = "Simulation",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Légende des marqueurs.
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ElementLegendeCarte(Flamme, "Ouvert")
                        ElementLegendeCarte(GrisInconnu, "Fermé")
                        if (simulation) {
                            ElementLegendeCarte(Color(0xFF1565C0), "Simulation")
                        }
                    }
                }
            }

            if (chargement) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
            }
        }

        // =================================================
        // BAS DE L'ÉCRAN : BOUTON POSITION + FICHE DU DÉPÔT
        // =================================================

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (positionClient != null) {
                FloatingActionButton(
                    onClick = { demandeRecentrage++ },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Ma position")
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            AnimatedVisibility(
                visible = depotSelectionne != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                depotSelectionne?.let { selection ->
                    FicheDepotCarte(
                        depotAvecDistance = selection,
                        context = context,
                        onFermer = { depotSelectionne = null },
                        onVoirDetail = { onDepotClique(selection) }
                    )
                }
            }

            // Crédit obligatoire des données OpenStreetMap.
            if (depotSelectionne == null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = "© contributeurs OpenStreetMap © CARTO",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// Fiche affichée en bas de la carte quand on touche un marqueur.
@Composable
private fun FicheDepotCarte(
    depotAvecDistance: DepotAvecDistance,
    context: Context,
    onFermer: () -> Unit,
    onVoirDetail: () -> Unit
) {
    val depot = depotAvecDistance.depot

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            PhotoDepot(
                depot = depot,
                forme = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            IconButton(
                onClick = onFermer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Fermer")
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = depot.nom.ifBlank { "Dépôt de gaz" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${depot.commune} · ${formaterDistance(depotAvecDistance.distanceMetres)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatutOuverture(estOuvert = depot.estOuvert)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${nombreEnStock(depot.stock)} bouteille(s) en stock",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onVoirDetail,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Voir le détail")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedButton(
                    onClick = { ouvrirItineraire(context, depot) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Filled.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Itinéraire")
                }
            }
        }
    }
}

@Composable
private fun ElementLegendeCarte(couleur: Color, texte: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(50),
            color = couleur,
            modifier = Modifier.size(10.dp)
        ) {}

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = texte,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
