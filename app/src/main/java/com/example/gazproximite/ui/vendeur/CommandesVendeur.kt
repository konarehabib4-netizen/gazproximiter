package com.example.gazproximite.ui.vendeur

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Commande
import com.example.gazproximite.data.model.ModeRemise
import com.example.gazproximite.data.model.StatutCommande
import com.example.gazproximite.ui.commandes.CarteCommande
import com.example.gazproximite.ui.commandes.formaterDateCommande
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.EtatVide
import com.example.gazproximite.ui.composants.TitreSection
import com.example.gazproximite.ui.composants.appelerDepot

// Nombre de commandes terminées / refusées / annulées affichées.
private const val COMMANDES_PASSEES_AFFICHEES = 20

// Onglet "Commandes" du vendeur : commandes reçues des clients.
@Composable
fun CommandesVendeur(
    context: Context,
    commandes: List<Commande>,
    onAccepter: (Commande) -> Unit,
    onRefuser: (Commande, String) -> Unit,
    onMarquerPrete: (Commande) -> Unit,
    onTerminer: (Commande) -> Unit
) {
    var aRefuser by remember { mutableStateOf<Commande?>(null) }
    var aTerminer by remember { mutableStateOf<Commande?>(null) }

    val nouvelles = commandes.filter { it.statutEnum == StatutCommande.EN_ATTENTE }
    val enCours = commandes.filter { it.statutEnum == StatutCommande.ACCEPTEE || it.statutEnum == StatutCommande.PRETE }
    val passees = commandes.filter { it.statutEnum.estFinale }.take(COMMANDES_PASSEES_AFFICHEES)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EnTeteDegrade(
                titre = "Commandes",
                sousTitre = "${nouvelles.size} nouvelle(s) · ${enCours.size} en cours"
            )
        }

        if (commandes.isEmpty()) {
            item {
                Box(modifier = Modifier.height(400.dp)) {
                    EtatVide(
                        icone = Icons.AutoMirrored.Outlined.ReceiptLong,
                        titre = "Aucune commande",
                        description = "Les commandes de vos clients apparaîtront ici dès leur envoi."
                    )
                }
            }
        }

        listOf(
            "Nouvelles commandes" to nouvelles,
            "En cours" to enCours,
            "Commandes passées" to passees
        ).forEach { (titre, liste) ->
            if (liste.isNotEmpty()) {
                item {
                    TitreSection(titre = titre, modifier = Modifier.padding(horizontal = 20.dp))
                }
                items(liste, key = { it.id }) { commande ->
                    CarteCommande(
                        commande = commande,
                        titre = commande.clientNom.ifBlank { "Client" },
                        sousTitre = "Reçue le ${formaterDateCommande(commande)}",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (commande.statutEnum == StatutCommande.REFUSEE && commande.motifRefus.isNotBlank()) {
                            Text(
                                "Motif du refus : ${commande.motifRefus}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        ActionsCommande(
                            context = context,
                            commande = commande,
                            onAccepter = { onAccepter(commande) },
                            onRefuser = { aRefuser = commande },
                            onMarquerPrete = { onMarquerPrete(commande) },
                            onTerminer = { aTerminer = commande }
                        )
                    }
                }
            }
        }
    }

    // Refus avec un motif transmis au client.
    aRefuser?.let { commande ->
        var motif by remember(commande.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { aRefuser = null },
            title = { Text("Refuser la commande ?") },
            text = {
                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    label = { Text("Motif (vu par le client)") },
                    placeholder = { Text("Ex. : rupture de B12, livraison impossible...") },
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRefuser(commande, motif)
                    aRefuser = null
                }) { Text("Refuser", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aRefuser = null }) { Text("Retour") } }
        )
    }

    // Confirmation avant de retirer les bouteilles du stock.
    aTerminer?.let { commande ->
        AlertDialog(
            onDismissRequest = { aTerminer = null },
            title = {
                Text(
                    if (commande.modeEnum == ModeRemise.LIVRAISON) "Commande livrée ?" else "Commande retirée ?"
                )
            },
            text = {
                Text(
                    "${commande.nombreBouteilles} bouteille(s) seront retirées de votre stock " +
                            "et la commande sera ajoutée à vos ventes (${formaterPrix(commande.total)})."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onTerminer(commande)
                    aTerminer = null
                }) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { aTerminer = null }) { Text("Retour") } }
        )
    }
}

// Boutons disponibles selon le statut de la commande.
@Composable
private fun ActionsCommande(
    context: Context,
    commande: Commande,
    onAccepter: () -> Unit,
    onRefuser: () -> Unit,
    onMarquerPrete: () -> Unit,
    onTerminer: () -> Unit
) {
    when (commande.statutEnum) {
        StatutCommande.EN_ATTENTE -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BoutonAction("Accepter", Icons.Filled.Check, onAccepter, Modifier.weight(1f))
            BoutonSecondaire("Refuser", Icons.Filled.Close, onRefuser, Modifier.weight(1f), danger = true)
        }

        StatutCommande.ACCEPTEE -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BoutonSecondaire("Prête", Icons.Filled.Inventory, onMarquerPrete, Modifier.weight(1f))
            BoutonAction("Terminer", Icons.Filled.DoneAll, onTerminer, Modifier.weight(1f))
        }

        StatutCommande.PRETE -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BoutonAction(
                if (commande.modeEnum == ModeRemise.LIVRAISON) "Livrée" else "Retirée",
                Icons.Filled.DoneAll,
                onTerminer,
                Modifier.weight(1f)
            )
            BoutonSecondaire("Refuser", Icons.Filled.Close, onRefuser, Modifier.weight(1f), danger = true)
        }

        else -> Unit
    }

    if (!commande.statutEnum.estFinale && commande.clientTelephone.isNotBlank()) {
        BoutonSecondaire(
            texte = "Appeler ${commande.clientTelephone}",
            icone = Icons.Filled.Phone,
            onClick = { appelerDepot(context, commande.clientTelephone) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BoutonAction(texte: String, icone: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Button(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Icon(icone, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texte)
    }
}

@Composable
private fun BoutonSecondaire(
    texte: String,
    icone: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    danger: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = if (danger) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonBorder(),
        colors = if (danger) {
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        modifier = modifier
    ) {
        Icon(icone, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texte)
    }
}
