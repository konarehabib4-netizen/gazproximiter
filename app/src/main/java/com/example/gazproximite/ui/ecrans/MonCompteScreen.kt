package com.example.gazproximite.ui.ecrans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.repository.ClientRepository
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mon compte",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Gérez vos informations personnelles",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Copie locale pour pouvoir utiliser le profil sans "?".
        val profilCharge = profil

        when {
            chargement -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Chargement de votre profil...")
            }

            profilCharge != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "👤",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${profilCharge.prenom} ${profilCharge.nom}".trim()
                                .ifBlank { "Client GazProximité" },
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text("📱 Téléphone")
                        Text(
                            text = profilCharge.telephone.ifBlank {
                                "Non renseigné"
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("📍 Adresse")
                        Text(
                            text = profilCharge.adresse.ifBlank {
                                "Non renseignée"
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Compte")
                        Text(
                            text = if (profilCharge.role == "client") {
                                "Client GazProximité"
                            } else {
                                profilCharge.role.ifBlank { "Non renseigné" }
                            }
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = messageErreur.ifBlank {
                        "Aucune information disponible."
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDeconnexion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Se déconnecter")
        }
    }
}
