package com.example.gazproximite.ui.carte

import android.content.Context
import android.location.Location
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gazproximite.R
import com.example.gazproximite.data.model.CENTRE_ABIDJAN
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.DepotAvecDistance
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint as PointCarte
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Fond de carte CARTO "Voyager" : données OpenStreetMap, gratuit et sans clé.
// Les serveurs d'OpenStreetMap (tile.openstreetmap.org) bloquent les applications,
// on utilise donc ces serveurs prévus pour les applis.
private val FOND_CARTE_CARTO = XYTileSource(
    "CartoVoyager",
    1,
    19,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
    ),
    "© OpenStreetMap © CARTO"
)

// Niveau de zoom pour voir une commune entière.
private const val ZOOM_VILLE = 12.5

// Niveau de zoom quand on se centre sur un point précis.
private const val ZOOM_PROCHE = 15.5

// Carte OpenStreetMap (bibliothèque osmdroid) intégrée dans Compose.
// Affiche un marqueur par dépôt et la position du client.
@Composable
fun CarteOsm(
    depots: List<DepotAvecDistance>,
    positionClient: Location?,
    afficherPositionClient: Boolean,
    depotSelectionne: Depot?,
    demandeRecentrage: Int,
    onMarqueurClique: (DepotAvecDistance) -> Unit,
    onCarteTouchee: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Crée la carte une seule fois.
    val carte = remember { creerCarte(context) }

    // Point bleu de la position du client.
    val calquePosition = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), carte)
    }

    // Toucher la carte en dehors d'un marqueur ferme la fiche du dépôt.
    val calqueEvenements = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: PointCarte?): Boolean {
                onCarteTouchee()
                return true
            }

            override fun longPressHelper(p: PointCarte?): Boolean = false
        })
    }

    // La carte doit suivre le cycle de vie de l'écran (pause / reprise).
    DisposableEffect(lifecycleOwner) {
        val observateur = LifecycleEventObserver { _, evenement ->
            when (evenement) {
                Lifecycle.Event.ON_RESUME -> carte.onResume()
                Lifecycle.Event.ON_PAUSE -> carte.onPause()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observateur)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observateur)
            calquePosition.disableMyLocation()
            carte.onDetach()
        }
    }

    // Affiche le point bleu seulement si la localisation est autorisée.
    LaunchedEffect(afficherPositionClient) {
        if (afficherPositionClient) {
            try {
                calquePosition.enableMyLocation()
                if (!carte.overlays.contains(calquePosition)) {
                    carte.overlays.add(calquePosition)
                }
            } catch (e: SecurityException) {
                // Permission retirée entre-temps : on n'affiche pas le point.
            }
        }
    }

    // Centre la carte sur le client dès que sa position est connue,
    // puis à chaque appui sur le bouton "Ma position".
    LaunchedEffect(positionClient, demandeRecentrage) {
        val position = positionClient ?: return@LaunchedEffect
        carte.controller.animateTo(
            PointCarte(position.latitude, position.longitude),
            if (demandeRecentrage == 0) ZOOM_VILLE + 1 else ZOOM_PROCHE,
            800L
        )
    }

    // Centre la carte sur le dépôt sélectionné.
    LaunchedEffect(depotSelectionne?.id) {
        val position = depotSelectionne?.position ?: return@LaunchedEffect
        carte.controller.animateTo(PointCarte(position.latitude, position.longitude))
    }

    AndroidView(
        factory = { carte },
        modifier = modifier,
        update = { vue ->
            // On remplace tous les marqueurs par la liste actuelle.
            vue.overlays.removeAll { it is Marker || it is MapEventsOverlay }
            vue.overlays.add(0, calqueEvenements)

            depots.forEach { depotAvecDistance ->
                vue.overlays.add(
                    creerMarqueur(vue, depotAvecDistance, onMarqueurClique)
                )
            }

            vue.invalidate()
        }
    )
}

// Prépare la vue osmdroid : fond de carte, zoom et centre sur Abidjan.
private fun creerCarte(context: Context): MapView {
    // osmdroid exige un "user agent" pour télécharger les tuiles de carte.
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName

        // Les tuiles téléchargées sont gardées dans le cache de l'application
        // (aucune permission de stockage n'est nécessaire).
        val dossierCarte = File(context.cacheDir, "osmdroid")
        osmdroidBasePath = dossierCarte
        osmdroidTileCache = File(dossierCarte, "tiles").apply { mkdirs() }
    }

    return MapView(context).apply {
        setTileSource(FOND_CARTE_CARTO)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = 5.0
        maxZoomLevel = 19.0
        isTilesScaledToDpi = true
        controller.setZoom(ZOOM_VILLE)
        controller.setCenter(PointCarte(CENTRE_ABIDJAN.latitude, CENTRE_ABIDJAN.longitude))
    }
}

// Crée le marqueur d'un dépôt : bleu en simulation, orange si ouvert, gris si fermé.
private fun creerMarqueur(
    carte: MapView,
    depotAvecDistance: DepotAvecDistance,
    onMarqueurClique: (DepotAvecDistance) -> Unit
): Marker {
    val depot = depotAvecDistance.depot
    val position = depot.position!!

    val icone = when {
        depot.estSimulation -> R.drawable.marqueur_depot_demo
        depot.estOuvert -> R.drawable.marqueur_depot_ouvert
        else -> R.drawable.marqueur_depot_ferme
    }

    return Marker(carte).apply {
        this.position = PointCarte(position.latitude, position.longitude)
        this.icon = ContextCompat.getDrawable(carte.context, icone)
        this.title = depot.nom
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // On remplace la bulle d'osmdroid par notre fiche Compose.
        setOnMarkerClickListener { _, _ ->
            onMarqueurClique(depotAvecDistance)
            true
        }
    }
}
