package com.example.gazproximite.ui.commandes

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Commande
import com.example.gazproximite.data.model.StatutCommande
import com.example.gazproximite.data.repository.CommandeRepository
import com.example.gazproximite.ui.composants.BandeauMessage
import com.example.gazproximite.ui.composants.Chargement
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.EtatVide
import com.example.gazproximite.ui.composants.TitreSection
import com.example.gazproximite.ui.composants.appelerDepot
import com.example.gazproximite.ui.ecrans.BoutonPrincipal
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Onglet "Commandes" du client : suivi en direct de ses commandes.
@Composable
fun MesCommandesScreen(
    context: Context,
    onExplorer: () -> Unit
) {
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val commandeRepository = remember { CommandeRepository() }
    val scope = rememberCoroutineScope()

    val resultat by remember(uid) {
        commandeRepository.observerCommandesClient(uid)
    }.collectAsState(initial = null)

    var aAnnuler by remember { mutableStateOf<Commande?>(null) }
    var message by remember { mutableStateOf("") }

    val commandes = resultat?.getOrNull().orEmpty()
    val enCours = commandes.filter { !it.statutEnum.estFinale }
    val historique = commandes.filter { it.statutEnum.estFinale }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EnTeteDegrade(
                titre = "Mes commandes",
                sousTitre = when {
                    resultat == null -> "Chargement..."
                    enCours.isNotEmpty() -> "${enCours.size} commande(s) en cours"
                    else -> "Suivez vos commandes de gaz en direct."
                }
            )
        }

        if (message.isNotBlank()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BandeauMessage(message = message, erreur = true)
                }
            }
        }

        when {
            resultat == null -> item {
                Chargement(texte = "Chargement de vos commandes...", modifier = Modifier.height(300.dp))
            }

            resultat?.isFailure == true -> item {
                Box(modifier = Modifier.height(380.dp)) {
                    EtatVide(
                        icone = Icons.Filled.CloudOff,
                        titre = "Commandes inaccessibles",
                        description = resultat?.exceptionOrNull()?.localizedMessage
                            ?: "Vérifiez votre connexion Internet."
                    )
                }
            }

            commandes.isEmpty() -> item {
                Box(modifier = Modifier.height(420.dp)) {
                    EtatVide(
                        icone = Icons.AutoMirrored.Outlined.ReceiptLong,
                        titre = "Aucune commande",
                        description = "Choisissez un dépôt puis touchez « Commander » pour réserver vos bouteilles."
                    ) {
                        BoutonPrincipal(texte = "Trouver un dépôt", onClick = onExplorer)
                    }
                }
            }

            else -> {
                listOf("En cours" to enCours, "Historique" to historique).forEach { (titre, liste) ->
                    if (liste.isNotEmpty()) {
                        item {
                            TitreSection(titre = titre, modifier = Modifier.padding(horizontal = 20.dp))
                        }
                        items(liste, key = { it.id }) { commande ->
                            CarteCommande(
                                commande = commande,
                                titre = commande.depotNom.ifBlank { "Dépôt de gaz" },
                                sousTitre = "Commandée le ${formaterDateCommande(commande)}",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = explicationStatutClient(commande),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = couleursStatut(commande.statutEnum).first
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (commande.depotTelephone.isNotBlank() && !commande.statutEnum.estFinale) {
                                        OutlinedButton(
                                            onClick = { appelerDepot(context, commande.depotTelephone) },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Appeler")
                                        }
                                    }
                                    if (commande.statutEnum == StatutCommande.EN_ATTENTE) {
                                        OutlinedButton(
                                            onClick = { aAnnuler = commande },
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Annuler")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    aAnnuler?.let { commande ->
        AlertDialog(
            onDismissRequest = { aAnnuler = null },
            title = { Text("Annuler la commande ?") },
            text = { Text("Votre commande chez ${commande.depotNom} sera annulée.") },
            confirmButton = {
                TextButton(onClick = {
                    aAnnuler = null
                    scope.launch {
                        commandeRepository.annulerParClient(commande.id)
                            .onSuccess { message = "" }
                            .onFailure { e -> message = e.localizedMessage ?: "Annulation impossible." }
                    }
                }) { Text("Annuler la commande", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { aAnnuler = null }) { Text("Garder") }
            }
        )
    }
}
