package com.example.gazproximite.ui.composants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gazproximite.R
import com.example.gazproximite.ui.theme.Flamme
import com.example.gazproximite.ui.theme.FlammeFonce
import com.example.gazproximite.ui.theme.GrisInconnu
import com.example.gazproximite.ui.theme.RougeRupture
import com.example.gazproximite.ui.theme.RougeRuptureClair
import com.example.gazproximite.ui.theme.VertDisponible
import com.example.gazproximite.ui.theme.VertDisponibleClair

// Dégradé orange utilisé pour les en-têtes et l'écran d'accueil.
val DegradeFlamme = Brush.linearGradient(
    colors = listOf(Color(0xFFF7893F), Flamme, FlammeFonce)
)

// =========================================================
// LOGO
// =========================================================

// Logo de l'application : bouteille de gaz blanche dans un cercle orange.
@Composable
fun LogoGaz(
    taille: Dp = 56.dp,
    fondBlanc: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(taille)
            .clip(CircleShape)
            .background(
                if (fondBlanc) {
                    Brush.linearGradient(listOf(Color.White, Color.White))
                } else {
                    DegradeFlamme
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bouteille_gaz),
            contentDescription = null,
            tint = if (fondBlanc) Flamme else Color.White,
            modifier = Modifier.size(taille * 0.55f)
        )
    }
}

// =========================================================
// EN-TÊTE
// =========================================================

// Bandeau orange en haut des écrans, avec coins arrondis en bas.
@Composable
fun EnTeteDegrade(
    titre: String,
    sousTitre: String? = null,
    contenu: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(DegradeFlamme)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = titre,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        if (sousTitre != null) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = sousTitre,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        contenu()
    }
}

// Titre de section ("Dépôts les plus proches", "Disponibilité"...).
@Composable
fun TitreSection(
    titre: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = titre,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

// =========================================================
// PASTILLES
// =========================================================

// Petite pastille colorée avec une icône et un texte.
@Composable
fun Pastille(
    texte: String,
    couleurTexte: Color,
    couleurFond: Color,
    icone: ImageVector? = null
) {
    Surface(
        shape = CircleShape,
        color = couleurFond
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icone != null) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = couleurTexte,
                    modifier = Modifier.size(14.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(couleurTexte)
                )

                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = texte,
                style = MaterialTheme.typography.labelMedium,
                color = couleurTexte
            )
        }
    }
}

// Pastille "Ouvert" (verte) ou "Fermé" (rouge).
@Composable
fun StatutOuverture(
    estOuvert: Boolean
) {
    Pastille(
        texte = if (estOuvert) "Ouvert" else "Fermé",
        couleurTexte = if (estOuvert) VertDisponible else RougeRupture,
        couleurFond = if (estOuvert) VertDisponibleClair else RougeRuptureClair
    )
}

// =========================================================
// LIGNE D'INFORMATION
// =========================================================

// Ligne "icône + libellé + valeur" utilisée dans les fiches.
@Composable
fun LigneInfo(
    icone: ImageVector,
    libelle: String,
    valeur: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconeRonde(icone = icone)

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = libelle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = valeur,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Icône sur un fond orange clair arrondi.
@Composable
fun IconeRonde(
    icone: ImageVector,
    taille: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(taille)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(taille * 0.5f)
        )
    }
}

// =========================================================
// CHAMP DE SAISIE
// =========================================================

// Champ de formulaire arrondi avec une icône à gauche.
@Composable
fun ChampTexte(
    valeur: String,
    onValeurChange: (String) -> Unit,
    libelle: String,
    icone: ImageVector,
    modifier: Modifier = Modifier,
    exemple: String? = null,
    actif: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onValeurChange,
        label = { Text(libelle) },
        placeholder = exemple?.let { { Text(it) } },
        leadingIcon = {
            Icon(imageVector = icone, contentDescription = null)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        enabled = actif,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

// =========================================================
// MESSAGE D'INFORMATION
// =========================================================

// Encadré coloré pour un message de succès ou d'erreur.
@Composable
fun BandeauMessage(
    message: String,
    erreur: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (erreur) RougeRuptureClair else VertDisponibleClair,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (erreur) RougeRupture else VertDisponible,
            modifier = Modifier.padding(14.dp)
        )
    }
}

// =========================================================
// ÉTAT VIDE
// =========================================================

// Écran affiché quand il n'y a rien à montrer :
// grande icône, titre, explication et boutons éventuels.
@Composable
fun EtatVide(
    icone: ImageVector,
    titre: String,
    description: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = titre,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            actions()
        }
    }
}

// Indicateur de chargement centré avec un texte.
@Composable
fun Chargement(
    texte: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = texte,
            style = MaterialTheme.typography.bodyMedium,
            color = GrisInconnu
        )
    }
}
