package com.example.gazproximite.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.gazproximite.R
import kotlinx.coroutines.delay

/**
 * [Ecran2Presentation] - Second écran de transition (Splash Screen 2).
 * Anime le déplacement vertical du logo vers le haut avant d'accéder à l'Onboarding.
 */
@Composable
fun Ecran2Presentation(
    onFinish: () -> Unit = {}
) {
    // ==========================================
    // --- SECTION 1 : ÉTATS ET ANIMATIONS ---
    // ==========================================
    // État de déclenchement de la sortie animée du logo
    var startExit by remember { mutableStateOf(false) }

    // Décalage vertical (offset Y) de 0.dp à -350.dp pour faire glisser le logo vers le haut
    val offsetY by animateDpAsState(
        targetValue = if (startExit) (-350).dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing), // Transition fluide accélérée
        label = "offsetYAnimation"
    )

    // Opacité (alpha) de 1.0 à 0.0 pour faire disparaître le logo en douceur
    val alpha by animateFloatAsState(
        targetValue = if (startExit) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "alphaAnimation"
    )

    // Séquence temporelle pour déclencher l'animation puis naviguer
    LaunchedEffect(Unit) {
        delay(400) // Maintien initial
        startExit = true // Lancement de la glissade vers le haut
        delay(250) // Temps d'attente de la fin de l'animation
        onFinish() // Callback de transition vers l'Onboarding
    }

    // ==========================================
    // --- SECTION 2 : CONTENEUR PRINCIPAL ---
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize() // Occupe toute la hauteur et largeur de la fenêtre
            .background(Color.White), // Fond blanc épuré pour la transition
        contentAlignment = Alignment.Center // Alignement centré initial pour le logo
    ) {
        // ==========================================
        // --- SECTION 3 : LOGO ANIMÉ EN TRANSITION ---
        // ==========================================
        Image(
            painter = painterResource(id = R.drawable.logo_yagaz2), // Logo principal YaGaz
            contentDescription = "Logo YaGaz en transition", // Accessibilité
            contentScale = ContentScale.Fit, // Adaptation sans déformation du logo
            modifier = Modifier
                .height(280.dp) // Hauteur identique au premier splash (280.dp) pour continuité visuelle
                .offset(y = offsetY) // Déplace l'élément sur l'axe Y selon la valeur animée
                .alpha(alpha) // Modifie la transparence de l'image
                .semantics { contentDescription = "Transition vers la présentation YaGaz" }
        )
    }
}
