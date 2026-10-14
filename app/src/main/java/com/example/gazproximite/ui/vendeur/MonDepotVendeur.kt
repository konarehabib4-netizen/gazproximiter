package com.example.gazproximite.ui.vendeur

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.ui.composants.ChampTexte
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.LigneInfo
import com.example.gazproximite.ui.composants.PhotoDepot
import com.example.gazproximite.ui.composants.TitreSection
import com.example.gazproximite.ui.ecrans.BoutonPrincipal

// Onglet "Mon dépôt" : ouverture, informations affichées aux clients et compte.
@Composable
fun MonDepotVendeur(
    vendeur: Utilisateur,
    depot: Depot,
    onBasculerOuverture: (Boolean) -> Unit,
    onEnregistrerInformations: (String, String, String, String, String, String) -> Unit,
    onDeconnexion: () -> Unit
) {
    // Les champs repartent des valeurs Firestore quand le dépôt change.
    var nom by remember(depot.nom) { mutableStateOf(depot.nom) }
    var commune by remember(depot.commune) { mutableStateOf(depot.commune) }
    var telephone by remember(depot.telephone) { mutableStateOf(depot.telephone) }
    var ouverture by remember(depot.heureOuverture) { mutableStateOf(depot.heureOuverture) }
    var fermeture by remember(depot.heureFermeture) { mutableStateOf(depot.heureFermeture) }
    var photoUrl by remember(depot.photoUrl) { mutableStateOf(depot.photoUrl) }

    val modifie = nom != depot.nom || commune != depot.commune || telephone != depot.telephone ||
            ouverture != depot.heureOuverture || fermeture != depot.heureFermeture ||
            photoUrl != depot.photoUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        EnTeteDegrade(
            titre = "Mon dépôt",
            sousTitre = "Ces informations sont affichées aux clients."
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Ouverture du dépôt
            CarteVendeur {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (depot.estOuvert) "Dépôt ouvert" else "Dépôt fermé",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (depot.estOuvert) {
                                "Les clients voient votre dépôt comme ouvert."
                            } else {
                                "Les clients voient votre dépôt comme fermé."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = depot.estOuvert, onCheckedChange = onBasculerOuverture)
                }
            }

            // Photo
            PhotoDepot(
                depot = depot.copy(photoUrl = photoUrl),
                forme = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            // Informations
            TitreSection(titre = "Informations")

            ChampTexte(valeur = nom, onValeurChange = { nom = it }, libelle = "Nom du dépôt", icone = Icons.Filled.Storefront)
            ChampTexte(valeur = commune, onValeurChange = { commune = it }, libelle = "Commune", icone = Icons.Filled.LocationCity)
            ChampTexte(
                valeur = telephone,
                onValeurChange = { telephone = it },
                libelle = "Téléphone",
                icone = Icons.Filled.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChampTexte(
                    valeur = ouverture,
                    onValeurChange = { ouverture = it },
                    libelle = "Ouverture",
                    icone = Icons.Filled.Schedule,
                    exemple = "07:00",
                    modifier = Modifier.weight(1f)
                )
                ChampTexte(
                    valeur = fermeture,
                    onValeurChange = { fermeture = it },
                    libelle = "Fermeture",
                    icone = Icons.Filled.Schedule,
                    exemple = "19:00",
                    modifier = Modifier.weight(1f)
                )
            }

            ChampTexte(
                valeur = photoUrl,
                onValeurChange = { photoUrl = it },
                libelle = "Lien de la photo (https://...)",
                icone = Icons.Filled.Image,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            BoutonPrincipal(
                texte = "Enregistrer les informations",
                actif = modifie && nom.isNotBlank(),
                onClick = { onEnregistrerInformations(nom, commune, telephone, ouverture, fermeture, photoUrl) }
            )

            // Compte du vendeur
            TitreSection(titre = "Mon compte")

            CarteVendeur {
                LigneInfo(
                    icone = Icons.Filled.Person,
                    libelle = "Responsable",
                    valeur = "${vendeur.prenom} ${vendeur.nom}".trim().ifBlank { "Non renseigné" }
                )
                LigneInfo(
                    icone = Icons.Filled.Email,
                    libelle = "E-mail",
                    valeur = vendeur.email.ifBlank { "Non renseigné" }
                )
            }

            OutlinedButton(
                onClick = onDeconnexion,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Se déconnecter", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
