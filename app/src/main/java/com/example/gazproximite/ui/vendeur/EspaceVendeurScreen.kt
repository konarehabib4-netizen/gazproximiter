package com.example.gazproximite.ui.vendeur

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import android.content.Context
import com.example.gazproximite.data.model.StatutCommande
import com.example.gazproximite.data.repository.CommandeRepository
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.repository.VendeurRepository
import com.example.gazproximite.ui.composants.Chargement
import com.example.gazproximite.ui.composants.EtatVide
import kotlinx.coroutines.launch

// Onglets de l'espace vendeur, dans l'ordre de la barre du bas.
enum class OngletVendeur(
    val libelle: String,
    val iconeSelectionnee: ImageVector,
    val icone: ImageVector
) {
    TABLEAU("Tableau", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    COMMANDES("Commandes", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    STOCK("Stock", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    VENTES("Ventes", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    DEPOT("Mon dépôt", Icons.Filled.Storefront, Icons.Outlined.Storefront)
}

// Espace du vendeur : son dépôt et ses ventes sont suivis en direct dans Firestore.
@Composable
fun EspaceVendeurScreen(
    context: Context,
    vendeur: Utilisateur,
    vendeurRepository: VendeurRepository,
    onDeconnexion: () -> Unit
) {
    val commandeRepository = remember { CommandeRepository() }

    var onglet by remember { mutableStateOf(OngletVendeur.TABLEAU) }

    // Suivi en direct du dépôt et des ventes.
    val resultatDepot by remember(vendeur.depotId) {
        vendeurRepository.observerDepot(vendeur.depotId)
    }.collectAsState(initial = null)

    val resultatVentes by remember(vendeur.depotId) {
        vendeurRepository.observerVentes(vendeur.depotId)
    }.collectAsState(initial = null)

    val ventes = resultatVentes?.getOrNull().orEmpty()

    // Commandes des clients, suivies en direct.
    val resultatCommandes by remember(vendeur.uid) {
        commandeRepository.observerCommandesVendeur(vendeur.uid)
    }.collectAsState(initial = null)

    val commandes = resultatCommandes?.getOrNull().orEmpty()
    val nouvellesCommandes = commandes.count { it.statutEnum == StatutCommande.EN_ATTENTE }

    // Messages courts en bas de l'écran (succès ou erreur).
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Lance une opération Firebase et affiche son résultat.
    fun executer(messageSucces: String?, operation: suspend () -> Result<Unit>) {
        scope.launch {
            operation()
                .onSuccess { if (messageSucces != null) snackbar.showSnackbar(messageSucces) }
                .onFailure { e -> snackbar.showSnackbar(e.localizedMessage ?: "Une erreur est survenue.") }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                OngletVendeur.entries.forEach { item ->
                    val selectionne = item == onglet
                    NavigationBarItem(
                        selected = selectionne,
                        onClick = { onglet = item },
                        icon = {
                            // Pastille rouge : nombre de nouvelles commandes.
                            BadgedBox(
                                badge = {
                                    if (item == OngletVendeur.COMMANDES && nouvellesCommandes > 0) {
                                        Badge { Text(nouvellesCommandes.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectionne) item.iconeSelectionnee else item.icone,
                                    contentDescription = item.libelle
                                )
                            }
                        },
                        label = { Text(item.libelle) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val resultat = resultatDepot
            val depot = resultat?.getOrNull()

            when {
                // Premier chargement en cours.
                resultat == null -> Chargement(texte = "Chargement de votre dépôt...")

                // Erreur Firestore (connexion, règles de sécurité...).
                resultat.isFailure -> EtatVide(
                    icone = Icons.Filled.CloudOff,
                    titre = "Dépôt inaccessible",
                    description = resultat.exceptionOrNull()?.localizedMessage
                        ?: "Vérifie ta connexion Internet."
                )

                // Le dépôt n'existe plus dans Firestore.
                depot == null -> EtatVide(
                    icone = Icons.Filled.Storefront,
                    titre = "Dépôt introuvable",
                    description = "Aucun dépôt n'est associé à ce compte vendeur."
                )

                else -> when (onglet) {
                    OngletVendeur.TABLEAU -> TableauBordVendeur(
                        vendeur = vendeur,
                        depot = depot,
                        ventes = ventes,
                        nouvellesCommandes = nouvellesCommandes,
                        onVoirCommandes = { onglet = OngletVendeur.COMMANDES },
                        onVoirStock = { onglet = OngletVendeur.STOCK },
                        onNouvelleVente = { onglet = OngletVendeur.VENTES }
                    )

                    OngletVendeur.COMMANDES -> CommandesVendeur(
                        context = context,
                        commandes = commandes,
                        onAccepter = { commande ->
                            executer("Commande acceptée.") { commandeRepository.accepter(commande.id) }
                        },
                        onRefuser = { commande, motif ->
                            executer("Commande refusée.") { commandeRepository.refuser(commande.id, motif) }
                        },
                        onMarquerPrete = { commande ->
                            executer("Commande prête.") { commandeRepository.marquerPrete(commande.id) }
                        },
                        onTerminer = { commande ->
                            executer("Commande terminée : stock et ventes mis à jour.") {
                                commandeRepository.terminer(commande.id)
                            }
                        }
                    )

                    OngletVendeur.STOCK -> StockVendeur(
                        depot = depot,
                        onModifierQuantite = { marque, format, quantite ->
                            executer(null) {
                                vendeurRepository.modifierQuantite(vendeur.depotId, marque, format, quantite)
                            }
                        },
                        onModifierPrix = { marque, format, prix ->
                            executer("Prix enregistré.") {
                                vendeurRepository.modifierPrix(vendeur.depotId, marque, format, prix)
                            }
                        }
                    )

                    OngletVendeur.VENTES -> VentesVendeur(
                        depot = depot,
                        ventes = ventes,
                        onEnregistrerVente = { marque, format, quantite, prix ->
                            executer("Vente enregistrée, le stock a été mis à jour.") {
                                vendeurRepository.enregistrerVente(vendeur.depotId, marque, format, quantite, prix)
                            }
                        },
                        onAnnulerVente = { vente ->
                            executer("Vente annulée, les bouteilles sont revenues en stock.") {
                                vendeurRepository.annulerVente(vendeur.depotId, vente)
                            }
                        }
                    )

                    OngletVendeur.DEPOT -> MonDepotVendeur(
                        vendeur = vendeur,
                        depot = depot,
                        onBasculerOuverture = { ouvert ->
                            executer(if (ouvert) "Dépôt ouvert aux clients." else "Dépôt fermé.") {
                                vendeurRepository.modifierOuverture(vendeur.depotId, ouvert)
                            }
                        },
                        onEnregistrerInformations = { nom, commune, telephone, ouverture, fermeture, photo ->
                            executer("Informations enregistrées.") {
                                vendeurRepository.modifierInformations(
                                    vendeur.depotId, nom, commune, telephone, ouverture, fermeture, photo
                                )
                            }
                        },
                        onDeconnexion = onDeconnexion
                    )
                }
            }
        }
    }
}
