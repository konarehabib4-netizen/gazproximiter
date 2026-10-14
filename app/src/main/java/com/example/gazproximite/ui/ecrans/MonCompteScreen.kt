package com.example.gazproximite.ui.ecrans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.repository.ClientRepository
import com.example.gazproximite.ui.composants.Chargement
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.EtatVide
import com.example.gazproximite.ui.composants.LigneInfo
import com.example.gazproximite.ui.composants.TitreSection
import com.example.gazproximite.ui.theme.Flamme
import com.google.firebase.auth.FirebaseAuth

// Onglet "Mon compte" : profil du client et déconnexion.
@Composable
fun MonCompteScreen(
    onDeconnexion: () -> Unit
) {
    // Récupérer l'identifiant du client actuellement connecté.
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // États de l'écran.
    var profil by remember {
        mutableStateOf<Utilisateur?>(null)
    }

    var chargement by remember {
        mutableStateOf(true)
    }

    var messageErreur by remember {
        mutableStateOf("")
    }

    // Charger le profil depuis Firestore.
    LaunchedEffect(uid) {
        if (uid == null) {
            messageErreur = "Aucun client connecté."
            chargement = false
        } else {
            ClientRepository()
                .recupererProfilClient(uid)
                .fold(
                    onSuccess = { donnees ->
                        profil = donnees

                        if (donnees == null) {
                            messageErreur =
                                "Votre profil n'a pas été trouvé."
                        }
                    },
                    onFailure = {
                        messageErreur =
                            "Impossible de charger votre profil."
                    }
                )

            chargement = false
        }
    }

    // Copie locale pour pouvoir utiliser le profil sans "?".
    val profilCharge = profil

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        EnTeteDegrade(
            titre = "Mon compte",
            sousTitre = "Gérez vos informations personnelles"
        ) {
            if (profilCharge != null) {
                Spacer(modifier = Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(profil = profilCharge)

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "${profilCharge.prenom} ${profilCharge.nom}".trim()
                                .ifBlank { "Client GazProximité" },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )

                        Text(
                            text = if (profilCharge.role == "client") {
                                "Client GazProximité"
                            } else {
                                profilCharge.role.ifBlank { "Compte" }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        when {
            chargement -> {
                Chargement(
                    texte = "Chargement de votre profil...",
                    modifier = Modifier.height(260.dp)
                )
            }

            profilCharge != null -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    TitreSection(
                        titre = "Informations",
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            LigneInfo(
                                icone = Icons.Filled.Phone,
                                libelle = "Téléphone",
                                valeur = profilCharge.telephone.ifBlank { "Non renseigné" }
                            )

                            LigneInfo(
                                icone = Icons.Filled.Home,
                                libelle = "Adresse",
                                valeur = profilCharge.adresse.ifBlank { "Non renseignée" }
                            )

                            LigneInfo(
                                icone = Icons.Filled.VerifiedUser,
                                libelle = "Type de compte",
                                valeur = if (profilCharge.role == "client") {
                                    "Client"
                                } else {
                                    profilCharge.role.ifBlank { "Non renseigné" }
                                }
                            )
                        }
                    }
                }
            }

            else -> {
                Box(modifier = Modifier.height(320.dp)) {
                    EtatVide(
                        icone = Icons.Filled.ErrorOutline,
                        titre = "Profil indisponible",
                        description = messageErreur.ifBlank {
                            "Aucune information disponible."
                        }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onDeconnexion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Se déconnecter", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Cercle blanc avec les initiales du client.
@Composable
private fun Avatar(profil: Utilisateur) {
    val initiales = listOf(profil.prenom, profil.nom)
        .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initiales,
            style = MaterialTheme.typography.titleLarge,
            color = Flamme
        )
    }
}
