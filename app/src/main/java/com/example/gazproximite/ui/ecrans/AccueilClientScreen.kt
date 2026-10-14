package com.example.gazproximite.ui.ecrans

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.location.formaterDistance
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.repository.ClientRepository
import com.example.gazproximite.ui.composants.CarteDepotAccueil
import com.example.gazproximite.ui.composants.Chargement
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.TitreSection
import com.google.firebase.auth.FirebaseAuth

// Nombre de dépôts affichés sur l'accueil.
private const val NOMBRE_DEPOTS_ACCUEIL = 5

// Onglet "Accueil" de l'espace client :
// affiche les dépôts les plus proches du client.
@Composable
fun AccueilClientScreen(
    context: Context,
    onDepotClique: (DepotAvecDistance) -> Unit,
    onVoirTout: () -> Unit
) {
    val etat = rememberDepotsProches(context)

    // Prénom du client, pour le message de bienvenue.
    var prenom by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        ClientRepository().recupererProfilClient(uid).onSuccess { profil ->
            prenom = profil?.prenom.orEmpty()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // En-tête orange avec les chiffres clés.
        item {
            EnTeteDegrade(
                titre = if (prenom.isBlank()) "Bonjour 👋" else "Bonjour $prenom 👋",
                sousTitre = "Trouvez votre bouteille de gaz près de chez vous."
            ) {
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CarteChiffre(
                        icone = Icons.Filled.Storefront,
                        valeur = if (etat.chargement) "…" else etat.depots.size.toString(),
                        libelle = "Dépôts",
                        modifier = Modifier.weight(1f)
                    )
                    CarteChiffre(
                        icone = Icons.Filled.Verified,
                        valeur = if (etat.chargement) "…" else
                            etat.depots.count { it.depot.estOuvert }.toString(),
                        libelle = "Ouverts",
                        modifier = Modifier.weight(1f)
                    )
                    CarteChiffre(
                        icone = Icons.Filled.NearMe,
                        valeur = etat.depots.firstOrNull()
                            ?.let { formaterDistance(it.distanceMetres) } ?: "—",
                        libelle = "Le plus proche",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        when {

            // Les dépôts sont en cours de récupération.
            etat.chargement -> item {
                Chargement(
                    texte = "Recherche des dépôts proches...",
                    modifier = Modifier.height(280.dp)
                )
            }

            // Aucun dépôt n'a été récupéré.
            etat.depots.isEmpty() -> item {
                Box(modifier = Modifier.height(420.dp)) {
                    MessageAucunDepot(etat = etat)
                }
            }

            // Affiche les dépôts les plus proches.
            else -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TitreSection(
                            titre = "Les plus proches",
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(onClick = onVoirTout) {
                            Text("Voir tout")
                        }
                    }
                }

                items(
                    items = etat.depots.take(NOMBRE_DEPOTS_ACCUEIL),
                    key = { depotAvecDistance ->
                        depotAvecDistance.depot.id
                    }
                ) { depotAvecDistance ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        CarteDepotAccueil(
                            depotAvecDistance = depotAvecDistance,
                            onClick = {
                                onDepotClique(depotAvecDistance)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Petite carte blanche avec un chiffre clé (nombre de dépôts, etc.).
@Composable
private fun CarteChiffre(
    icone: ImageVector,
    valeur: String,
    libelle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = valeur,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = libelle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
