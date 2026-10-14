package com.example.gazproximite.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.gazproximite.R
import com.example.gazproximite.ui.theme.*
import kotlinx.coroutines.delay

/**
 * [Ecran1Splash] - Premier écran de démarrage (Splash Screen) de l'application YaGaz.
 * Affiche le logo animé avec effet de zoom et de transparence avant de passer à l'écran suivant.
 */
@Composable
fun Ecran1Splash(
    onFinish: () -> Unit = {}
) {
    // ==========================================
    // --- SECTION 1 : ÉTATS ET ANIMATIONS ---
    // ==========================================
    // État déclencheur des animations lors du chargement de l'écran
    var startAnimation by remember { mutableStateOf(false) }

    // Animation de fondu (alpha : 0.0 -> 1.0) sur 300 millisecondes
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "alphaAnimation"
    )

    // Animation d'échelle (scale : 0.85 -> 1.0) avec un effet de rebond dynamique
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // Niveau de rebond moyen
            stiffness = Spring.StiffnessMedium // Raideur du ressort moyenne
        ),
        label = "scaleAnimation"
    )

    // Déclenchement automatique de l'animation et de la transition vers l'écran suivant
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(600) // Pause de 600ms pour apprécier le logo
        onFinish() // Transition vers le Splash 2 (Présentation)
    }

    // ==========================================
    // --- SECTION 2 : CONTENEUR PRINCIPAL DU SPLASH ---
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize() // Occupe 100% de la surface disponible de l'écran
            .clip(RoundedCornerShape(24.dp)) // Découpe les bords avec un arrondi de 24.dp
            .background(Color(0xFF102949)) // Couleur de fond #102949
            .semantics { contentDescription = "Écran de démarrage YaGaz" }, // Description d'accessibilité (TalkBack)
        contentAlignment = Alignment.Center // Centre le logo au milieu parfait de l'écran
    ) {
        // ==========================================
        // --- SECTION 3 : LOGO ANIMÉ YAGAZ ---
        // ==========================================
        Image(
            painter = painterResource(id = R.drawable.logo_yagaz2), // Ressource vectorielle/image du logo
            contentDescription = "Logo officiel YaGaz", // Description pour les lecteurs d'écran
            contentScale = ContentScale.Fit, // Conserve le ratio d'aspect sans déformation
            modifier = Modifier
                .height(280.dp) // Hauteur fixe de 280.dp pour une lisibilité idéale
                .alpha(alphaAnim) // Modifie l'opacité progressivement (transparence -> opaque)
                .scale(scaleAnim) // Modifie l'échelle avec l'effet de rebond
        )
    }
}
