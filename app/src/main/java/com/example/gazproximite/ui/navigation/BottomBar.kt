package com.example.gazproximite.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Onglets de l'espace client, dans l'ordre de la barre du bas.
// Chaque onglet a une icône pleine (sélectionné) et une icône contour.
enum class OngletClient(
    val libelle: String,
    val iconeSelectionnee: ImageVector,
    val icone: ImageVector
) {
    ACCUEIL("Accueil", Icons.Filled.Home, Icons.Outlined.Home),
    CARTE("Carte", Icons.Filled.Map, Icons.Outlined.Map),
    DEPOTS("Dépôts", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    COMMANDES("Commandes", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    MON_COMPTE("Compte", Icons.Filled.Person, Icons.Outlined.Person)
}

// Barre de navigation en bas de l'espace client.
@Composable
fun BottomBar(
    ongletSelectionne: OngletClient,
    onOngletClique: (OngletClient) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        OngletClient.entries.forEach { onglet ->
            val selectionne = onglet == ongletSelectionne

            NavigationBarItem(
                selected = selectionne,
                onClick = { onOngletClique(onglet) },
                icon = {
                    Icon(
                        imageVector = if (selectionne) onglet.iconeSelectionnee else onglet.icone,
                        contentDescription = onglet.libelle
                    )
                },
                label = { Text(onglet.libelle) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
