package com.example.gazproximite.ui.vendeur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.model.Vente
import com.example.gazproximite.ui.composants.BadgeMarque
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.StatutOuverture
import com.example.gazproximite.ui.composants.TitreSection
import com.example.gazproximite.ui.theme.RougeRupture

// Onglet "Tableau" : l'essentiel de la journée en un coup d'œil.
@Composable
fun TableauBordVendeur(
    vendeur: Utilisateur,
    depot: Depot,
    ventes: List<Vente>,
    nouvellesCommandes: Int,
    onVoirCommandes: () -> Unit,
    onVoirStock: () -> Unit,
    onNouvelleVente: () -> Unit
) {
    val ventesJour = ventesDuJour(ventes)
    val bouteillesVenduesJour = ventesJour.sumOf { it.quantite }
    val chiffreJour = ventesJour.sumOf { it.total }

    // Bouteilles presque épuisées (entre 1 et le seuil).
    val stocksFaibles = Marque.entries.flatMap { marque ->
        Format.entries.mapNotNull { format ->
            val quantite = quantiteEnStock(depot, marque, format)
            if (quantite in 1..SEUIL_STOCK_FAIBLE) Triple(marque, format, quantite) else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        EnTeteDegrade(
            titre = "Bonjour ${vendeur.prenom.ifBlank { "vendeur" }} 👋",
            sousTitre = depot.nom
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            StatutOuverture(estOuvert = depot.estOuvert)
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CarteChiffreVendeur(
                    icone = Icons.Filled.ShoppingCart,
                    valeur = bouteillesVenduesJour.toString(),
                    libelle = "Vendues aujourd'hui",
                    modifier = Modifier.weight(1f)
                )
                CarteChiffreVendeur(
                    icone = Icons.Filled.Inventory2,
                    valeur = totalBouteilles(depot).toString(),
                    libelle = "En stock",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            CarteChiffreVendeur(
                icone = Icons.Filled.Payments,
                valeur = formaterPrix(chiffreJour),
                libelle = "Chiffre d'affaires du jour",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Actions rapides
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onNouvelleVente,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vendre")
                }
                OutlinedButton(
                    onClick = onVoirStock,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mon stock")
                }
            }

            // Nouvelles commandes à traiter
            if (nouvellesCommandes > 0) {
                Button(
                    onClick = onVoirCommandes,
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "$nouvellesCommandes nouvelle(s) commande(s) à traiter",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Alertes de stock
            if (stocksFaibles.isNotEmpty()) {
                CarteVendeur {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFE6A100))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stock faible",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    stocksFaibles.forEach { (marque, format, quantite) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BadgeMarque(marque = marque, taille = 28.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${format.name} ${marque.libelle}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "plus que $quantite",
                                style = MaterialTheme.typography.labelLarge,
                                color = RougeRupture
                            )
                        }
                    }
                }
            }

            // Dernières ventes
            TitreSection(titre = "Dernières ventes")

            if (ventes.isEmpty()) {
                Text(
                    text = "Aucune vente enregistrée pour le moment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CarteVendeur {
                    ventes.take(5).forEachIndexed { index, vente ->
                        LigneVente(vente = vente)
                        if (index < minOf(ventes.size, 5) - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

// Ligne d'une vente : marque, quantité, date et montant.
@Composable
fun LigneVente(
    vente: Vente,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val marque = vente.marqueEnum
        if (marque != null) {
            BadgeMarque(marque = marque, taille = 34.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${vente.quantite} × ${libelleVente(vente)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formaterDateVente(vente),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formaterPrix(vente.total),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        actions()
    }
}

// Petite carte blanche avec un chiffre clé.
@Composable
private fun CarteChiffreVendeur(
    icone: ImageVector,
    valeur: String,
    libelle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
