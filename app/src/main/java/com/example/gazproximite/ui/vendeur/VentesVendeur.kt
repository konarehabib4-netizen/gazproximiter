@file:OptIn(ExperimentalLayoutApi::class)

package com.example.gazproximite.ui.vendeur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.Vente
import com.example.gazproximite.ui.composants.BadgeMarque
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.TitreSection
import com.example.gazproximite.ui.theme.RougeRupture

// Onglet "Ventes" : enregistrer une vente et consulter l'historique.
@Composable
fun VentesVendeur(
    depot: Depot,
    ventes: List<Vente>,
    onEnregistrerVente: (Marque, Format, Long, Long) -> Unit,
    onAnnulerVente: (Vente) -> Unit
) {
    var nouvelleVente by remember { mutableStateOf(false) }
    var venteAAnnuler by remember { mutableStateOf<Vente?>(null) }

    val ventesJour = ventesDuJour(ventes)
    val idsJour = ventesJour.map { it.id }.toSet()
    val anciennes = ventes.filter { it.id !in idsJour }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EnTeteDegrade(
                titre = "Mes ventes",
                sousTitre = "Aujourd'hui : ${ventesJour.sumOf { it.quantite }} bouteille(s) · " +
                        formaterPrix(ventesJour.sumOf { it.total })
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { nouvelleVente = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nouvelle vente", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (ventes.isEmpty()) {
            item {
                Text(
                    text = "Aucune vente pour le moment. Appuyez sur « Nouvelle vente » " +
                            "à chaque bouteille vendue : le stock se met à jour tout seul.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        listOf("Aujourd'hui" to ventesJour, "Ventes précédentes" to anciennes).forEach { (titre, liste) ->
            if (liste.isNotEmpty()) {
                item {
                    TitreSection(titre = titre, modifier = Modifier.padding(horizontal = 20.dp))
                }
                item {
                    CarteVendeur(modifier = Modifier.padding(horizontal = 16.dp)) {
                        liste.forEachIndexed { index, vente ->
                            LigneVente(vente = vente) {
                                IconButton(onClick = { venteAAnnuler = vente }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Annuler cette vente",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (index < liste.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (nouvelleVente) {
        DialogueNouvelleVente(
            depot = depot,
            onAnnuler = { nouvelleVente = false },
            onValider = { marque, format, quantite, prix ->
                onEnregistrerVente(marque, format, quantite, prix)
                nouvelleVente = false
            }
        )
    }

    venteAAnnuler?.let { vente ->
        AlertDialog(
            onDismissRequest = { venteAAnnuler = null },
            title = { Text("Annuler cette vente ?") },
            text = {
                Text(
                    "${vente.quantite} × ${libelleVente(vente)} (${formaterPrix(vente.total)}) " +
                            "sera supprimée et les bouteilles reviendront dans le stock."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAnnulerVente(vente)
                    venteAAnnuler = null
                }) { Text("Annuler la vente", color = RougeRupture) }
            },
            dismissButton = {
                TextButton(onClick = { venteAAnnuler = null }) { Text("Garder") }
            }
        )
    }
}

// Saisie d'une vente : bouteille, quantité et prix.
@Composable
private fun DialogueNouvelleVente(
    depot: Depot,
    onAnnuler: () -> Unit,
    onValider: (Marque, Format, Long, Long) -> Unit
) {
    // Par défaut : la première bouteille disponible en stock.
    val premiereDispo = Marque.entries.flatMap { m -> Format.entries.map { f -> m to f } }
        .firstOrNull { (m, f) -> quantiteEnStock(depot, m, f) > 0 }

    var marque by remember { mutableStateOf(premiereDispo?.first ?: Marque.PETROCI) }
    var format by remember { mutableStateOf(premiereDispo?.second ?: Format.B12) }
    var quantite by remember { mutableLongStateOf(1L) }
    var prixTexte by remember(marque, format) {
        mutableStateOf(prixBouteille(depot, marque, format).takeIf { it > 0 }?.toString().orEmpty())
    }

    val enStock = quantiteEnStock(depot, marque, format)
    val prix = prixTexte.toLongOrNull() ?: 0L
    val valide = quantite in 1..enStock && prix > 0

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvelle vente") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Marque", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Marque.entries.forEach { m ->
                        FilterChip(
                            selected = m == marque,
                            onClick = { marque = m; quantite = 1 },
                            label = { Text(m.libelle) },
                            leadingIcon = { BadgeMarque(marque = m, taille = 18.dp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Text("Format", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Format.entries.forEach { f ->
                        FilterChip(
                            selected = f == format,
                            onClick = { format = f; quantite = 1 },
                            label = { Text("${f.name} (${quantiteEnStock(depot, marque, f)})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Text(
                    text = if (enStock > 0) "$enStock bouteille(s) en stock" else "Rupture de stock pour cette bouteille",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enStock > 0) MaterialTheme.colorScheme.onSurfaceVariant else RougeRupture
                )

                // Quantité vendue
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantité", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(
                        onClick = { if (quantite > 1) quantite-- },
                        enabled = quantite > 1,
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Moins") }
                    Text(
                        text = quantite.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    FilledTonalIconButton(
                        onClick = { if (quantite < enStock) quantite++ },
                        enabled = quantite < enStock,
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Filled.Add, contentDescription = "Plus") }
                }

                OutlinedTextField(
                    value = prixTexte,
                    onValueChange = { saisie -> prixTexte = saisie.filter { it.isDigit() }.take(7) },
                    label = { Text("Prix d'une bouteille (F CFA)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Total : ${formaterPrix(quantite * prix)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(marque, format, quantite, prix) },
                enabled = valide
            ) { Text("Enregistrer la vente") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}
