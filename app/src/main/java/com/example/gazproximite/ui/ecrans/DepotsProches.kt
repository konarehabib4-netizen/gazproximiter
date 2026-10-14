package com.example.gazproximite.ui.ecrans

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.gazproximite.data.location.LocationRepository
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.repository.DepotDistanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Résultat du chargement des dépôts proches.
data class EtatDepotsProches(
    val depots: List<DepotAvecDistance> = emptyList(),
    val chargement: Boolean = true,
    val messageErreur: String = ""
)

// Demande la permission de localisation, puis charge les dépôts
// triés du plus proche au plus éloigné.
//
// Utilisé par l'accueil du client et par la liste des dépôts,
// pour ne pas écrire deux fois la même logique.
@Composable
fun rememberDepotsProches(context: Context): EtatDepotsProches {

    var etat by remember {
        mutableStateOf(EtatDepotsProches())
    }

    // Indique si la permission de localisation est accordée.
    var localisationAutorisee by remember {
        mutableStateOf(false)
    }

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
                    "La localisation est nécessaire pour afficher les dépôts proches."
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

    // Charge les dépôts dès que la localisation est autorisée.
    LaunchedEffect(localisationAutorisee) {

        if (!localisationAutorisee) {
            return@LaunchedEffect
        }

        etat = EtatDepotsProches(chargement = true)

        etat = try {
            // Récupération des dépôts et calcul des distances.
            val depots = withContext(Dispatchers.IO) {
                DepotDistanceRepository(context)
                    .recupererDepotsAvecDistance()
            }

            EtatDepotsProches(
                depots = depots,
                chargement = false,
                messageErreur = if (depots.isEmpty()) {
                    "Aucun dépôt localisable trouvé. Vérifie ta position et les coordonnées des dépôts."
                } else {
                    ""
                }
            )

        } catch (e: Exception) {
            EtatDepotsProches(
                chargement = false,
                messageErreur =
                    "Impossible de charger les dépôts. Vérifie ta connexion Internet."
            )
        }
    }

    return etat
}
