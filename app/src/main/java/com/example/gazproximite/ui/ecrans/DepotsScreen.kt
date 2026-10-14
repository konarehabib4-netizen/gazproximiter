package com.example.gazproximite.ui.ecrans

import android.content.Context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.gazproximite.ui.composants.BarreRecherche
import com.example.gazproximite.ui.composants.CarteDepot

import java.util.Locale

// Normalise le texte pour faciliter la recherche.
// Exemple : "Total Energies" devient "totalenergies".
private fun normaliserTexte(texte: String): String {
    return texte
        .trim()
        .lowercase(Locale.ROOT)
        .filterNot {
            it.isWhitespace() || it == '_' || it == '-'
        }
}

// Écran qui affiche tous les dépôts disponibles.
@Composable
fun DepotsScreen(context: Context) {

    // Dépôts proches, chargés après la permission de localisation.
    val etat = rememberDepotsProches(context)
    val depots = etat.depots

    // Texte saisi dans la barre de recherche.
    var recherche by remember {
        mutableStateOf("")
    }

    // Normalise le texte saisi.
    val rechercheNormalisee = normaliserTexte(recherche)

    // Filtre les dépôts selon les informations et les stocks.
    val depotsFiltres = depots.filter { depotAvecDistance ->

        val depot = depotAvecDistance.depot

        // Recherche dans le nom, la commune et le téléphone.
        val correspondInformations =
            normaliserTexte(depot.nom).contains(rechercheNormalisee) ||
                    normaliserTexte(depot.commune).contains(rechercheNormalisee) ||
                    normaliserTexte(depot.telephone).contains(rechercheNormalisee)

        // Recherche dans les clés de stock.
        // Exemples : petroci_b12, totalenergies_b6.
        val correspondStock = depot.stock.keys.any { cle ->
            normaliserTexte(cle).contains(rechercheNormalisee)
        }

        // Si la recherche est vide, tous les dépôts sont affichés.
        rechercheNormalisee.isEmpty() ||
                correspondInformations ||
                correspondStock
    }

    // Construction de l'interface.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "GazProximité",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Trouvez un dépôt près de vous",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        BarreRecherche(
            recherche = recherche,
            onRechercheChange = { recherche = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Liste des dépôts",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Affichage adapté à l'état du chargement.
        when {

            // Les dépôts sont en cours de récupération.
            etat.chargement -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Recherche des dépôts proches...")
                }
            }

            // Aucun dépôt n'a été récupéré.
            depots.isEmpty() -> {
                Text(
                    text = etat.messageErreur.ifBlank {
                        "Aucun dépôt disponible pour le moment."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Aucun résultat ne correspond à la recherche.
            depotsFiltres.isEmpty() -> {
                Text(
                    text = "Aucun dépôt ne correspond à votre recherche.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Affiche la liste des dépôts filtrés.
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = depotsFiltres,
                        key = { depotAvecDistance ->
                            depotAvecDistance.depot.id
                        }
                    ) { depotAvecDistance ->
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
