package com.example.gazproximite.ui.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Onglets de l'espace client, dans l'ordre de la barre du bas.
enum class OngletClient(val icone: String, val libelle: String) {
    ACCUEIL("⌂", "Accueil"),
    DEPOTS("▤", "Dépôts"),
    FAVORIS("♡", "Favoris"),
    MON_COMPTE("♙", "Mon compte")
}

// Barre de navigation en bas de l'espace client.
@Composable
fun BottomBar(
    ongletSelectionne: OngletClient,
    onOngletClique: (OngletClient) -> Unit
) {
    NavigationBar {
        OngletClient.entries.forEach { onglet ->
            NavigationBarItem(
                selected = onglet == ongletSelectionne,
                onClick = { onOngletClique(onglet) },
                icon = { Text(onglet.icone) },
                label = { Text(onglet.libelle) }
            )
        }
    }
}
