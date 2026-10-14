package com.example.gazproximite.ui.ecrans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gazproximite.ui.composants.EnTeteDegrade
import com.example.gazproximite.ui.composants.EtatVide

// Onglet "Favoris" : écran provisoire en attendant la fonctionnalité.
@Composable
fun FavorisScreen(
    onExplorer: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        EnTeteDegrade(
            titre = "Mes favoris",
            sousTitre = "Retrouvez rapidement vos dépôts préférés."
        )

        EtatVide(
            icone = Icons.Outlined.FavoriteBorder,
            titre = "Aucun favori pour l'instant",
            description = "Les dépôts que vous ajouterez en favoris apparaîtront ici."
        ) {
            BoutonPrincipal(
                texte = "Explorer les dépôts",
                onClick = onExplorer
            )
        }
    }
}
