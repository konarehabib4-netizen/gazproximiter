package com.example.gazproximite.ui.commandes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Commande
import com.example.gazproximite.data.model.ModeRemise
import com.example.gazproximite.data.model.StatutCommande
import com.example.gazproximite.ui.composants.BadgeMarque
import com.example.gazproximite.ui.composants.Pastille
import com.example.gazproximite.ui.theme.RougeRupture
import com.example.gazproximite.ui.theme.RougeRuptureClair
import com.example.gazproximite.ui.theme.VertDisponible
import com.example.gazproximite.ui.theme.VertDisponibleClair
import com.example.gazproximite.ui.vendeur.formaterPrix
import java.text.SimpleDateFormat
import java.util.Locale

// Couleurs (texte, fond) de chaque statut.
fun couleursStatut(statut: StatutCommande): Pair<Color, Color> = when (statut) {
    StatutCommande.EN_ATTENTE -> Color(0xFFB26A00) to Color(0xFFFFF1D6)
    StatutCommande.ACCEPTEE -> Color(0xFF1565C0) to Color(0xFFE1ECFA)
    StatutCommande.PRETE -> Color(0xFF6A1B9A) to Color(0xFFF1E4F7)
    StatutCommande.TERMINEE -> VertDisponible to VertDisponibleClair
    StatutCommande.REFUSEE -> RougeRupture to RougeRuptureClair
    StatutCommande.ANNULEE -> Color(0xFF6B7280) to Color(0xFFEEF0F3)
}

// Pastille colorée du statut d'une commande.
@Composable
fun PastilleStatut(statut: StatutCommande) {
    val (texte, fond) = couleursStatut(statut)
    Pastille(texte = statut.libelle, couleurTexte = texte, couleurFond = fond)
}

// Date d'une commande, ex. "14/10 à 15:42".
fun formaterDateCommande(commande: Commande): String {
    val date = commande.creeLe?.toDate() ?: return "à l'instant"
    return SimpleDateFormat("dd/MM 'à' HH:mm", Locale.FRANCE).format(date)
}

// Explication du statut pour le client.
fun explicationStatutClient(commande: Commande): String = when (commande.statutEnum) {
    StatutCommande.EN_ATTENTE -> "Le dépôt n'a pas encore répondu à votre commande."
    StatutCommande.ACCEPTEE -> "Le dépôt prépare votre commande."
    StatutCommande.PRETE -> if (commande.modeEnum == ModeRemise.LIVRAISON) {
        "Votre commande est prête et va être livrée."
    } else {
        "Votre commande est prête : vous pouvez passer la récupérer."
    }
    StatutCommande.TERMINEE -> "Commande remise. Merci !"
    StatutCommande.REFUSEE -> "Le dépôt a refusé la commande" +
            if (commande.motifRefus.isNotBlank()) " : ${commande.motifRefus}" else "."
    StatutCommande.ANNULEE -> "Vous avez annulé cette commande."
}

// Carte d'une commande : en-tête, bouteilles, remise, total et actions.
@Composable
fun CarteCommande(
    commande: Commande,
    titre: String,
    sousTitre: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // En-tête
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titre,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sousTitre,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PastilleStatut(statut = commande.statutEnum)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Bouteilles
            commande.lignes.forEach { ligne ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ligne.marqueEnum?.let { marque ->
                        BadgeMarque(marque = marque, taille = 26.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = ligne.libelle,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (ligne.prixUnitaire > 0) formaterPrix(ligne.total) else "Prix au dépôt",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Remise
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (commande.modeEnum == ModeRemise.LIVRAISON) {
                        Icons.Filled.DeliveryDining
                    } else {
                        Icons.Filled.Storefront
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (commande.modeEnum == ModeRemise.LIVRAISON) {
                        "Livraison : ${commande.adresseLivraison}"
                    } else {
                        "Retrait au dépôt"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (commande.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = commande.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${commande.nombreBouteilles} bouteille(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (commande.total > 0) formaterPrix(commande.total) else "Total au dépôt",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}
