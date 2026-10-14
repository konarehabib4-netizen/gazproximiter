package com.example.gazproximite.ui.ecrans

import android.content.Context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.cleStock
import com.example.gazproximite.ui.composants.BadgeMarque
import com.example.gazproximite.ui.composants.BarreRecherche
import com.example.gazproximite.ui.composants.CarteDepot
import com.example.gazproximite.ui.composants.Chargement
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.EtatVide

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
fun DepotsScreen(
    context: Context,
    onDepotClique: (DepotAvecDistance) -> Unit = {}
) {

    // Dépôts proches, chargés après la permission de localisation.
    val etat = rememberDepotsProches(context)
    val depots = etat.depots

    // Texte saisi dans la barre de recherche.
    var recherche by remember {
        mutableStateOf("")
    }

    // Marque choisie dans les filtres (null = toutes les marques).
    var marqueFiltre by remember {
        mutableStateOf<Marque?>(null)
    }

    // Normalise le texte saisi.
    val rechercheNormalisee = normaliserTexte(recherche)

    // Filtre les dépôts selon les informations, les stocks et la marque.
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

        // Si une marque est choisie, le dépôt doit en avoir au moins un format en stock.
        val correspondMarque = marqueFiltre?.let { marque ->
            Format.entries.any { format -> depot.stock[cleStock(marque, format)] == true }
        } ?: true

        // Si la recherche est vide, tous les dépôts sont affichés.
        correspondMarque && (
                rechercheNormalisee.isEmpty() ||
                        correspondInformations ||
                        correspondStock
                )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // En-tête avec la barre de recherche.
        item {
            EnTeteDegrade(
                titre = "Dépôts de gaz",
                sousTitre = if (etat.chargement) {
                    "Recherche en cours..."
                } else {
                    "${depotsFiltres.size} dépôt(s) autour de vous"
                }
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                BarreRecherche(
                    recherche = recherche,
                    onRechercheChange = { recherche = it }
                )
            }
        }

        // Filtres par marque.
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = marqueFiltre == null,
                        onClick = { marqueFiltre = null },
                        label = { Text("Toutes") },
                        colors = couleursFiltre()
                    )
                }

                items(Marque.entries) { marque ->
                    FilterChip(
                        selected = marqueFiltre == marque,
                        onClick = {
                            marqueFiltre = if (marqueFiltre == marque) null else marque
                        },
                        label = { Text(marque.libelle) },
                        leadingIcon = { BadgeMarque(marque = marque, taille = 20.dp) },
                        colors = couleursFiltre()
                    )
                }
            }
        }

        // Affichage adapté à l'état du chargement.
        when {

            // Les dépôts sont en cours de récupération.
            etat.chargement -> item {
                Chargement(
                    texte = "Recherche des dépôts proches...",
                    modifier = Modifier.height(320.dp)
                )
            }

            // Aucun dépôt n'a été récupéré.
            depots.isEmpty() -> item {
                Box(modifier = Modifier.height(440.dp)) {
                    MessageAucunDepot(etat = etat)
                }
            }

            // Aucun résultat ne correspond à la recherche.
            depotsFiltres.isEmpty() -> item {
                Box(modifier = Modifier.height(360.dp)) {
                    EtatVide(
                        icone = Icons.Filled.SearchOff,
                        titre = "Aucun résultat",
                        description = "Aucun dépôt ne correspond à votre recherche. " +
                                "Essayez un autre mot ou une autre marque."
                    )
                }
            }

            // Affiche la liste des dépôts filtrés.
            else -> items(
                items = depotsFiltres,
                key = { depotAvecDistance ->
                    depotAvecDistance.depot.id
                }
            ) { depotAvecDistance ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CarteDepot(
                        depotAvecDistance = depotAvecDistance,
                        context = context,
                        onClick = { onDepotClique(depotAvecDistance) }
                    )
                }
            }
        }
    }
}

@Composable
private fun couleursFiltre() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor = MaterialTheme.colorScheme.surface
)
