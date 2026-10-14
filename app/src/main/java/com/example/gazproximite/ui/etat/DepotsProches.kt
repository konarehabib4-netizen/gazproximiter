package com.example.gazproximite.ui.etat

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.gazproximite.data.location.LocationRepository
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.repository.DepotDistanceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DepotsProches"

// Résultat du chargement des dépôts proches.
data class EtatDepotsProches(
    val depots: List<DepotAvecDistance> = emptyList(),
    val chargement: Boolean = true,
    val messageErreur: String = "",

    // Vrai quand la localisation (GPS) est coupée dans les réglages.
    val localisationDesactivee: Boolean = false,

    // Relance la recherche des dépôts (bouton "Réessayer").
    val recharger: () -> Unit = {}
)

// Demande la permission de localisation, puis charge les dépôts
// triés du plus proche au plus éloigné.
// Avec inclureSimulation, les dépôts fictifs de démonstration sont ajoutés.
@Composable
fun rememberDepotsProches(
    context: Context,
    inclureSimulation: Boolean = false
): EtatDepotsProches {

    var etat by remember {
        mutableStateOf(EtatDepotsProches())
    }

    // Indique si la permission de localisation est accordée.
    var localisationAutorisee by remember {
        mutableStateOf(false)
    }

    // Chaque incrément relance le chargement.
    var tentative by remember {
        mutableIntStateOf(0)
    }

    val recharger: () -> Unit = { tentative++ }

    // Demande les permissions de localisation à Android.
    val demandePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val autorisee =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        localisationAutorisee = autorisee

        if (!autorisee) {
            etat = EtatDepotsProches(
                chargement = false,
                messageErreur =
                    "La localisation est nécessaire pour afficher les dépôts proches.",
                recharger = recharger
            )
        }
    }

    // Vérifie la permission au premier affichage de l'écran.
    LaunchedEffect(Unit) {
        if (LocationRepository(context).localisationAutorisee()) {
            localisationAutorisee = true
        } else {
            demandePermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Charge les dépôts dès que la localisation est autorisée,
    // puis à chaque nouvelle tentative ou changement du mode démonstration.
    LaunchedEffect(localisationAutorisee, tentative, inclureSimulation) {

        if (!localisationAutorisee) {
            return@LaunchedEffect
        }

        // Sans GPS activé, inutile d'attendre une position.
        if (!LocationRepository(context).localisationActivee()) {
            etat = EtatDepotsProches(
                chargement = false,
                messageErreur =
                    "La localisation de ton téléphone est désactivée. " +
                            "Active-la pour voir les dépôts proches.",
                localisationDesactivee = true,
                recharger = recharger
            )
            return@LaunchedEffect
        }

        etat = EtatDepotsProches(chargement = true, recharger = recharger)

        etat = try {
            // Récupération des dépôts et calcul des distances.
            val depots = withContext(Dispatchers.IO) {
                DepotDistanceRepository(context)
                    .recupererDepotsAvecDistance(inclureSimulation)
            }

            EtatDepotsProches(
                depots = depots.orEmpty(),
                chargement = false,
                messageErreur = when {
                    depots == null ->
                        "Impossible d'obtenir ta position. Vérifie que le GPS capte bien, puis réessaie."

                    depots.isEmpty() ->
                        "Aucun dépôt enregistré pour le moment. Active le mode démonstration dans ton profil pour voir des exemples."

                    else -> ""
                },
                recharger = recharger
            )

        } catch (e: CancellationException) {
            // L'écran a été quitté pendant le chargement : ce n'est pas une erreur.
            throw e
        } catch (e: Exception) {
            // Le détail de l'erreur est visible dans Logcat (filtre "DepotsProches").
            Log.e(TAG, "Échec du chargement des dépôts", e)

            EtatDepotsProches(
                chargement = false,
                messageErreur =
                    "Impossible de charger les dépôts : ${e.localizedMessage ?: "erreur inconnue"}",
                recharger = recharger
            )
        }
    }

    return etat
}
