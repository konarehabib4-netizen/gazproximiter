package com.example.gazproximite.ui.ecrans

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.location.LocationRepository
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.repository.DepotDistanceRepository
import com.example.gazproximite.ui.composants.EtatVide
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
    // puis à chaque nouvelle tentative.
    LaunchedEffect(localisationAutorisee, tentative) {

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
                    .recupererDepotsAvecDistance()
            }

            EtatDepotsProches(
                depots = depots.orEmpty(),
                chargement = false,
                messageErreur = when {
                    depots == null ->
                        "Impossible d'obtenir ta position. Vérifie que le GPS capte bien, puis réessaie."

                    depots.isEmpty() ->
                        "Aucun dépôt avec des coordonnées n'a été trouvé dans la base."

                    else -> ""
                },
                recharger = recharger
            )

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

// Message affiché quand aucun dépôt n'a pu être chargé,
// avec les boutons pour corriger le problème.
@Composable
fun MessageAucunDepot(
    etat: EtatDepotsProches
) {
    // Ouvre les réglages de localisation, puis recharge au retour.
    val ouvrirReglages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        etat.recharger()
    }

    EtatVide(
        icone = if (etat.localisationDesactivee) {
            Icons.Filled.LocationOff
        } else {
            Icons.Filled.SearchOff
        },
        titre = if (etat.localisationDesactivee) {
            "Localisation désactivée"
        } else {
            "Aucun dépôt trouvé"
        },
        description = etat.messageErreur.ifBlank {
            "Aucun dépôt disponible pour le moment."
        }
    ) {
        if (etat.localisationDesactivee) {
            Button(
                onClick = {
                    ouvrirReglages.launch(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Activer la localisation")
            }
        }

        OutlinedButton(
            onClick = etat.recharger,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Réessayer")
        }
    }
}
