
package ui.ecrans
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.example.gazproximite.data.location.LocationRepository
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.repository.DepotDistanceRepository

import java.util.Locale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Liste des marques de gaz proposées dans l'application.
private val marquesGaz = listOf(
    "Petroci",
    "TotalEnergies",
    "Shell",
    "Oryx",
    "Corlay"
)

// Liste des formats de bouteilles disponibles.
private val formatsGaz = listOf(
    "B6",
    "B12",
    "B28"
)

// Écran principal affichant les dépôts au client.
@Composable
fun DepotsScreen(
    context: Context,
    onDeconnexion: () -> Unit
) {
    // Contient les dépôts récupérés avec leur distance.
    var depots by remember {
        mutableStateOf<List<DepotAvecDistance>>(emptyList())
    }

    // Indique si les données sont en cours de chargement.
    var chargement by remember {
        mutableStateOf(true)
    }

    // Contient un éventuel message d'erreur.
    var messageErreur by remember {
        mutableStateOf("")
    }

    // Indique si l'autorisation de localisation est accordée.
    var localisationAutorisee by remember {
        mutableStateOf(false)
    }

    // Lance la demande de permission officielle d'Android.
    val demandePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        // L'accès peut être précis ou approximatif.
        val autorisee =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        localisationAutorisee = autorisee

        // Si le client refuse, explique pourquoi les dépôts proches
        // ne peuvent pas être calculés.
        if (!autorisee) {
            chargement = false
            messageErreur =
                "La localisation est nécessaire pour afficher les dépôts proches."
        }
    }

    // Au premier affichage, vérifie automatiquement la permission.
    LaunchedEffect(Unit) {
        val locationRepository = LocationRepository(context)

        if (locationRepository.localisationAutorisee()) {
            // L'autorisation existe déjà : on peut charger les dépôts.
            localisationAutorisee = true
        } else {
            // Android demande l'autorisation au client.
            demandePermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Lorsque l'autorisation est disponible, récupère les dépôts.
    LaunchedEffect(localisationAutorisee) {
        if (!localisationAutorisee) {
            return@LaunchedEffect
        }

        chargement = true
        messageErreur = ""

        try {
            // Le chargement s'effectue hors du fil principal de l'interface.
            // Le dépôt de données calcule les distances et trie les résultats.
            depots = withContext(Dispatchers.IO) {
                DepotDistanceRepository(context)
                    .recupererDepotsAvecDistance()
            }

            // Une liste vide peut signifier qu'aucune position exploitable
            // ou aucun dépôt avec coordonnées n'a été trouvé.
            if (depots.isEmpty()) {
                messageErreur =
                    "Aucun dépôt localisable trouvé. Vérifie la position du téléphone et les coordonnées des dépôts."
            }
        } catch (e: Exception) {
            // Évite de faire planter l'interface en cas d'erreur.
            messageErreur =
                "Impossible de charger les dépôts. Vérifie ta connexion Internet."
        } finally {
            // Termine toujours l'indicateur de chargement.
            chargement = false
        }
    }

    // Construction de l'interface.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Titre de l'écran.
        Text(
            text = "Dépôts proches",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Les dépôts sont classés du plus proche au plus éloigné.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bouton permettant au client de quitter sa session.
        Button(
            onClick = onDeconnexion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Déconnexion")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Choisit le contenu à afficher selon l'état du chargement.
        when {
            chargement -> {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(8.dp))

                Text("Recherche des dépôts proches...")
            }

            depots.isEmpty() -> {
                // Affiche l'erreur si aucun dépôt n'est disponible.
                Text(
                    text = messageErreur,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                // LazyColumn affiche les dépôts sans charger inutilement
                // toutes les cartes d'un seul coup.
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = depots,
                        key = { it.depot.id }
                    ) { depotAvecDistance ->

                        // Une carte représente un dépôt.
                        CarteDepot(
                            depotAvecDistance = depotAvecDistance,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

// Carte affichant les informations d'un dépôt.
@Composable
private fun CarteDepot(
    depotAvecDistance: DepotAvecDistance,
    context: Context
) {
    // Récupère les informations du dépôt.
    val depot = depotAvecDistance.depot

    // Distance calculée entre le client et le dépôt, en mètres.
    val distance = depotAvecDistance.distanceMetres

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Nom du dépôt.
            Text(
                text = depot.nom,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Commune où se situe le dépôt.
            Text(text = "Commune : ${depot.commune}")

            // Convertit la distance pour qu'elle soit plus lisible.
            val distanceTexte = if (distance < 1000) {
                "${distance.toInt()} m"
            } else {
                String.format(
                    Locale.FRANCE,
                    "%.2f km",
                    distance / 1000.0
                )
            }

            Text(text = "Distance : $distanceTexte")

            Spacer(modifier = Modifier.height(4.dp))

            // Indique si le dépôt est ouvert ou fermé.
            Text(
                text = if (depot.estOuvert) {
                    "Dépôt ouvert"
                } else {
                    "Dépôt fermé"
                },
                color = if (depot.estOuvert) {
                    Color(0xFF2E7D32)
                } else {
                    Color(0xFFC62828)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Disponibilité des bouteilles",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Parcourt les cinq marques.
            marquesGaz.forEach { marque ->

                Text(
                    text = marque,
                    style = MaterialTheme.typography.titleSmall
                )

                // Pour chaque marque, affiche B6, B12 et B28.
                formatsGaz.forEach { format ->

                    // Exemple :
                    // "TotalEnergies" + "B12" donne "totalenergies_b12".
                    val cleStock =
                        "${marque.lowercase(Locale.ROOT)}_${format.lowercase(Locale.ROOT)}"

                    // Lit la disponibilité dans la map stock du dépôt.
                    LigneStock(
                        format = format,
                        disponible = depot.stock[cleStock]
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Affiche le téléphone uniquement s'il est renseigné.
            if (depot.telephone.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Appeler le dépôt : ${depot.telephone}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,

                    // Rend le numéro cliquable.
                    modifier = Modifier.clickable {
                        val numero = depot.telephone.trim()

                        // ACTION_DIAL ouvre le composeur téléphonique
                        // sans lancer automatiquement l'appel.
                        val intent = Intent(
                            Intent.ACTION_DIAL,
                            Uri.fromParts("tel", numero, null)
                        )

                        // Ouvre l'application Téléphone du dispositif.
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

// Affiche le stock d'un format de bouteille.
@Composable
private fun LigneStock(
    format: String,
    disponible: Boolean?
) {
    // Interprétation de la valeur enregistrée dans Firestore.
    val texte = when (disponible) {
        true -> "En stock"
        false -> "Rupture"
        null -> "Information indisponible"
    }

    // Une couleur différente permet de repérer rapidement le stock.
    val couleur = when (disponible) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Format de la bouteille : B6, B12 ou B28.
        Text(text = format)

        // État du stock : en stock, rupture ou information manquante.
        Text(
            text = texte,
            color = couleur
        )
    }
}
