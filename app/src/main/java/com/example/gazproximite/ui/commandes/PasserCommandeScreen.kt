package com.example.gazproximite.ui.commandes

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.LigneCommande
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.ModeRemise
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.model.cleStock
import com.example.gazproximite.data.repository.ClientRepository
import com.example.gazproximite.data.repository.CommandeRepository
import com.example.gazproximite.ui.composants.BadgeMarque
import com.example.gazproximite.ui.composants.BandeauMessage
import com.example.gazproximite.ui.composants.ChampTexte
import com.example.gazproximite.ui.composants.DegradeFlamme
import com.example.gazproximite.ui.composants.EtatVide
import com.example.gazproximite.ui.composants.TitreSection
import com.example.gazproximite.ui.composants.appelerDepot
import com.example.gazproximite.ui.ecrans.BoutonPrincipal
import com.example.gazproximite.ui.vendeur.CarteVendeur
import com.example.gazproximite.ui.vendeur.formaterPrix
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip

// Quantité maximale proposée quand le dépôt n'indique pas son stock exact.
private const val QUANTITE_MAX_PAR_DEFAUT = 10L

// Bouteille proposée à la commande.
private data class BouteilleProposee(
    val marque: Marque,
    val format: Format,
    val prix: Long,
    val maximum: Long
)

// Bouteilles en stock dans ce dépôt, avec leur prix et la quantité maximale.
private fun bouteillesProposees(depot: Depot): List<BouteilleProposee> =
    Marque.entries.flatMap { marque ->
        Format.entries.mapNotNull { format ->
            val cle = cleStock(marque, format)
            val quantite = depot.quantites[cle]
            val maximum = when {
                quantite != null -> quantite
                depot.stock[cle] == true -> QUANTITE_MAX_PAR_DEFAUT
                else -> 0L
            }
            if (maximum > 0) {
                BouteilleProposee(marque, format, depot.prix[cle] ?: 0L, maximum)
            } else {
                null
            }
        }
    }

// Écran où le client compose et envoie sa commande au dépôt.
@Composable
fun PasserCommandeScreen(
    depot: Depot,
    context: Context,
    onRetour: () -> Unit,
    onCommandeEnvoyee: () -> Unit
) {
    val commandeRepository = remember { CommandeRepository() }
    val scope = rememberCoroutineScope()

    // Profil du client (nom et téléphone transmis au vendeur).
    var client by remember { mutableStateOf<Utilisateur?>(null) }

    // Quantité choisie pour chaque bouteille (clé "marque_format").
    val quantites = remember { mutableStateMapOf<String, Long>() }

    var mode by remember { mutableStateOf(ModeRemise.RETRAIT) }
    var adresse by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var envoi by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf("") }
    var envoyee by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        ClientRepository().recupererProfilClient(uid).onSuccess { profil ->
            client = profil
            if (adresse.isBlank()) adresse = profil?.adresse.orEmpty()
        }
    }

    val proposees = remember(depot) { bouteillesProposees(depot) }
    val lignes = proposees.mapNotNull { bouteille ->
        val quantite = quantites[cleStock(bouteille.marque, bouteille.format)] ?: 0L
        if (quantite > 0) {
            LigneCommande(
                marque = bouteille.marque.name.lowercase(),
                format = bouteille.format.name.lowercase(),
                quantite = quantite,
                prixUnitaire = bouteille.prix
            )
        } else {
            null
        }
    }
    val total = lignes.sumOf { it.total }
    val prixManquant = lignes.any { it.prixUnitaire == 0L }

    // Commande envoyée : écran de confirmation.
    if (envoyee) {
        EtatVide(
            icone = Icons.Filled.CheckCircle,
            titre = "Commande envoyée !",
            description = "${depot.nom} a reçu votre commande. " +
                    "Suivez son avancement dans l'onglet « Commandes »."
        ) {
            BoutonPrincipal(texte = "Voir mes commandes", onClick = onCommandeEnvoyee)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // En-tête
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(DegradeFlamme)
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            IconButton(
                onClick = onRetour,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Passer une commande", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(depot.nom, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
        }

        // Dépôt qui ne prend pas les commandes en ligne.
        if (depot.estSimulation || depot.proprietaireUid.isBlank()) {
            EtatVide(
                icone = Icons.Filled.RemoveShoppingCart,
                titre = "Commande en ligne indisponible",
                description = if (depot.estSimulation) {
                    "Ce dépôt est un exemple de démonstration : il ne reçoit pas de commandes."
                } else {
                    "Ce dépôt n'a pas encore d'espace vendeur. Appelez-le pour commander."
                },
                modifier = Modifier.height(420.dp)
            ) {
                if (depot.telephone.isNotBlank()) {
                    BoutonPrincipal(
                        texte = "Appeler le dépôt",
                        onClick = { appelerDepot(context, depot.telephone) }
                    )
                }
            }
            return@Column
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!depot.estOuvert) {
                BandeauMessage(
                    message = "Ce dépôt est fermé pour le moment : votre commande sera traitée à sa réouverture.",
                    erreur = true
                )
            }

            TitreSection(titre = "Vos bouteilles")

            if (proposees.isEmpty()) {
                Text(
                    text = "Aucune bouteille en stock dans ce dépôt pour le moment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CarteVendeur {
                    proposees.forEachIndexed { index, bouteille ->
                        val cle = cleStock(bouteille.marque, bouteille.format)
                        val quantite = quantites[cle] ?: 0L

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BadgeMarque(marque = bouteille.marque, taille = 34.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${bouteille.format.name} ${bouteille.marque.libelle}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (bouteille.prix > 0) formaterPrix(bouteille.prix) else "Prix au dépôt",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { quantites[cle] = (quantite - 1).coerceAtLeast(0) },
                                enabled = quantite > 0,
                                modifier = Modifier.size(34.dp)
                            ) { Icon(Icons.Filled.Remove, contentDescription = "Moins") }
                            Text(
                                text = quantite.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            FilledTonalIconButton(
                                onClick = { quantites[cle] = quantite + 1 },
                                enabled = quantite < bouteille.maximum,
                                modifier = Modifier.size(34.dp)
                            ) { Icon(Icons.Filled.Add, contentDescription = "Plus") }
                        }

                        if (index < proposees.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            TitreSection(titre = "Remise")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeRemise.entries.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = { mode = option },
                        label = { Text(option.libelle) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            if (mode == ModeRemise.LIVRAISON) {
                ChampTexte(
                    valeur = adresse,
                    onValeurChange = { adresse = it },
                    libelle = "Adresse de livraison",
                    icone = Icons.Filled.Home,
                    exemple = "Ex. : Cocody Angré, près de la pharmacie"
                )
            }

            ChampTexte(
                valeur = note,
                onValeurChange = { note = it },
                libelle = "Note pour le vendeur (facultatif)",
                icone = Icons.AutoMirrored.Filled.Notes,
                exemple = "Ex. : je passe vers 18 h"
            )

            client?.telephone?.takeIf { it.isNotBlank() }?.let { telephone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Le vendeur pourra vous joindre au $telephone",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Total
            CarteVendeur {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${lignes.sumOf { it.quantite }} bouteille(s)",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formaterPrix(total),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (prixManquant) {
                    Text(
                        "Certaines bouteilles n'ont pas de prix affiché : le montant exact sera confirmé par le dépôt.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (erreur.isNotBlank()) {
                BandeauMessage(message = erreur, erreur = true)
            }

            BoutonPrincipal(
                texte = "Envoyer la commande",
                chargement = envoi,
                actif = lignes.isNotEmpty() && client != null,
                onClick = {
                    val profil = client ?: return@BoutonPrincipal
                    envoi = true
                    erreur = ""
                    scope.launch {
                        commandeRepository.passerCommande(
                            depot = depot,
                            client = profil,
                            lignes = lignes,
                            mode = mode,
                            adresseLivraison = if (mode == ModeRemise.LIVRAISON) adresse else "",
                            note = note
                        ).onSuccess {
                            envoi = false
                            envoyee = true
                        }.onFailure { e ->
                            envoi = false
                            erreur = "Commande impossible : ${e.localizedMessage ?: "erreur inconnue"}"
                        }
                    }
                }
            )
        }
    }
}
