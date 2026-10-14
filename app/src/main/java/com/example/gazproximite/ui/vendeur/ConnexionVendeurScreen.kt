package com.example.gazproximite.ui.vendeur

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.repository.VendeurRepository
import com.example.gazproximite.ui.composants.BandeauMessage
import com.example.gazproximite.ui.composants.ChampTexte
import com.example.gazproximite.ui.ecrans.BoutonPrincipal
import com.example.gazproximite.ui.ecrans.GabaritAuth
import kotlinx.coroutines.launch

// Connexion du vendeur avec son e-mail et son mot de passe.
@Composable
fun ConnexionVendeurScreen(
    vendeurRepository: VendeurRepository,
    onRetour: () -> Unit,
    onConnexionReussie: (Utilisateur) -> Unit,
    onCreerDepot: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var chargement by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    GabaritAuth(
        titre = "Espace vendeur",
        sousTitre = "Connectez-vous pour gérer le stock et les ventes de votre dépôt.",
        icone = Icons.Filled.Storefront,
        onRetour = onRetour,
        retourActif = !chargement
    ) {
        ChampTexte(
            valeur = email,
            onValeurChange = { email = it; erreur = "" },
            libelle = "Adresse e-mail",
            icone = Icons.Filled.Email,
            exemple = "vendeur@exemple.ci",
            actif = !chargement,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        ChampMotDePasse(
            valeur = motDePasse,
            onValeurChange = { motDePasse = it; erreur = "" },
            actif = !chargement
        )

        BoutonPrincipal(
            texte = "Se connecter",
            chargement = chargement,
            onClick = {
                if (email.isBlank() || motDePasse.isBlank()) {
                    erreur = "Renseigne ton e-mail et ton mot de passe."
                    return@BoutonPrincipal
                }

                chargement = true
                scope.launch {
                    vendeurRepository.connecter(email, motDePasse)
                        .onSuccess { vendeur ->
                            chargement = false
                            onConnexionReussie(vendeur)
                        }
                        .onFailure { e ->
                            chargement = false
                            erreur = "Connexion impossible : ${e.localizedMessage ?: "vérifie tes identifiants"}"
                        }
                }
            }
        )

        if (erreur.isNotBlank()) {
            BandeauMessage(message = erreur, erreur = true)
        }

        TextButton(onClick = onCreerDepot, enabled = !chargement) {
            Text("Nouveau vendeur ? Créer mon dépôt")
        }
    }
}
