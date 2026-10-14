package com.example.gazproximite.ui.vendeur

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.ui.composants.BadgeMarque
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.Pastille
import com.example.gazproximite.ui.theme.RougeRupture
import com.example.gazproximite.ui.theme.RougeRuptureClair
import com.example.gazproximite.ui.theme.VertDisponible
import com.example.gazproximite.ui.theme.VertDisponibleClair

// Valeur en cours de saisie dans la boîte de dialogue.
private data class Edition(
    val marque: Marque,
    val format: Format,
    val estPrix: Boolean,
    val valeurActuelle: Long
)

// Onglet "Stock" : quantité et prix de chaque bouteille.
@Composable
fun StockVendeur(
    depot: Depot,
    onModifierQuantite: (Marque, Format, Long) -> Unit,
    onModifierPrix: (Marque, Format, Long) -> Unit
) {
    var edition by remember { mutableStateOf<Edition?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EnTeteDegrade(
                titre = "Mon stock",
                sousTitre = "${totalBouteilles(depot)} bouteille(s) en stock. " +
                        "Touchez une quantité ou un prix pour le saisir."
            )
        }

        items(Marque.entries) { marque ->
            CarteVendeur(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgeMarque(marque = marque, taille = 36.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = marque.libelle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Format.entries.forEachIndexed { index, format ->
                    LigneStockVendeur(
                        format = format,
                        quantite = quantiteEnStock(depot, marque, format),
                        prix = prixBouteille(depot, marque, format),
                        onMoins = {
                            val actuelle = quantiteEnStock(depot, marque, format)
                            if (actuelle > 0) onModifierQuantite(marque, format, actuelle - 1)
                        },
                        onPlus = {
                            onModifierQuantite(marque, format, quantiteEnStock(depot, marque, format) + 1)
                        },
                        onSaisirQuantite = {
                            edition = Edition(marque, format, false, quantiteEnStock(depot, marque, format))
                        },
                        onSaisirPrix = {
                            edition = Edition(marque, format, true, prixBouteille(depot, marque, format))
                        }
                    )

                    if (index < Format.entries.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    // Boîte de dialogue de saisie d'une quantité ou d'un prix.
    edition?.let { courante ->
        DialogueSaisie(
            edition = courante,
            onAnnuler = { edition = null },
            onValider = { valeur ->
                if (courante.estPrix) {
                    onModifierPrix(courante.marque, courante.format, valeur)
                } else {
                    onModifierQuantite(courante.marque, courante.format, valeur)
                }
                edition = null
            }
        )
    }
}

// Une ligne : format, statut, prix, et boutons − / +.
@Composable
private fun LigneStockVendeur(
    format: Format,
    quantite: Long,
    prix: Long,
    onMoins: () -> Unit,
    onPlus: () -> Unit,
    onSaisirQuantite: () -> Unit,
    onSaisirPrix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = format.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (quantite > 0) {
                    Pastille(texte = "En stock", couleurTexte = VertDisponible, couleurFond = VertDisponibleClair)
                } else {
                    Pastille(texte = "Rupture", couleurTexte = RougeRupture, couleurFond = RougeRuptureClair)
                }
            }

            Text(
                text = if (prix > 0) formaterPrix(prix) else "Prix non renseigné",
                style = MaterialTheme.typography.labelMedium,
                color = if (prix > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onSaisirPrix)
                    .padding(vertical = 4.dp)
            )
        }

        FilledTonalIconButton(
            onClick = onMoins,
            enabled = quantite > 0,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Retirer une bouteille")
        }

        Surface(
            onClick = onSaisirQuantite,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .widthIn(min = 48.dp)
        ) {
            Text(
                text = quantite.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        FilledTonalIconButton(
            onClick = onPlus,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Ajouter une bouteille")
        }
    }
}

// Saisie au clavier d'une quantité ou d'un prix.
@Composable
private fun DialogueSaisie(
    edition: Edition,
    onAnnuler: () -> Unit,
    onValider: (Long) -> Unit
) {
    var texte by remember { mutableStateOf(if (edition.valeurActuelle > 0) edition.valeurActuelle.toString() else "") }
    val valeur = texte.toLongOrNull()

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = {
            Text(
                if (edition.estPrix) {
                    "Prix ${edition.format.name} ${edition.marque.libelle}"
                } else {
                    "Stock ${edition.format.name} ${edition.marque.libelle}"
                }
            )
        },
        text = {
            OutlinedTextField(
                value = texte,
                onValueChange = { saisie -> texte = saisie.filter { it.isDigit() }.take(7) },
                label = { Text(if (edition.estPrix) "Prix en F CFA" else "Nombre de bouteilles") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { valeur?.let(onValider) },
                enabled = valeur != null
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}
