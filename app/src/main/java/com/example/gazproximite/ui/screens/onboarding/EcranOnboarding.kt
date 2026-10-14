package com.example.gazproximite.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gazproximite.R
import com.example.gazproximite.ui.screens.auth.EcranAuthSheet
import com.example.gazproximite.ui.theme.*
import kotlinx.coroutines.launch

// ==========================================
// --- DIMENSIONS & PROPRIÉTÉS LOCALES ---
// ==========================================
private val OnboardingHorizontalPadding: Dp = 24.dp
private val OnboardingIndicatorActiveWidth: Dp = 24.dp
private val OnboardingIndicatorInactiveWidth: Dp = 8.dp
private val OnboardingIndicatorHeight: Dp = 8.dp
private val OnboardingButtonHeight: Dp = 56.dp
private val OnboardingButtonCornerRadius: Dp = 25.dp
private val OnboardingIllustrationMaxHeight: Dp = 280.dp

/**
 * [EcranOnboarding] - Écran de présentation guidée et d'introduction de l'application YaGaz.
 * Présente le service à travers 4 pages défilantes et inclut la feuille d'authentification.
 */
@Composable
fun EcranOnboarding(
    onLoginSuccess: () -> Unit = {},
    onCreerCompte: () -> Unit = {},
    ouvrirConnexion: Boolean = false,
) {
    // ==========================================
    // --- SECTION 1 : ÉTATS ET CONTRÔLEURS ---
    // ==========================================
    // Venant de la page d'inscription ("Se connecter"), on ouvre directement la connexion.
    val pagerState = rememberPagerState(
        initialPage = if (ouvrirConnexion) 3 else 0,
        pageCount = { 4 }
    )
    val coroutineScope = rememberCoroutineScope()

    // Contrôle de l'affichage du panneau BottomSheet d'authentification
    var showAuthSheet by remember { mutableStateOf(ouvrirConnexion) }

    // ==========================================
    // --- SECTION 2 : STRUCTURATION PRINCIPALE DE L'ÉCRAN ---
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(OnboardingHorizontalPadding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // --- SECTION 3 : EN-TÊTE LOGO YAGAZ & BOUTON PASSER ---
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-35).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_yagaz2),
                    contentDescription = "Logo officiel YaGaz",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(130.dp)
                )

                if (pagerState.currentPage < 3) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.semantics { contentDescription = "Passer la présentation et aller au dernier écran" }
                    ) {
                        Text(
                            text = "Passer",
                            fontFamily = ComfortaaBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextGrayBlue,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // ==========================================
            // --- SECTION 4 : PAGER HORIZONTAL (4 PAGES DE PRÉSENTATION) ---
            // ==========================================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        imageRes = R.drawable.image1,
                        imageDesc = "Illustration de recherche de points de vente de gaz",
                        titleContent = {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bienvenue sur ",
                                    fontFamily = NunitoBold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 30.sp,
                                    color = TextGrayBlue,
                                    style = TextStyle(lineHeight = 36.sp)
                                )
                                Text(
                                    text = "YaGaz",
                                    fontFamily = NunitoBold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 30.sp,
                                    color = YaDarkBlue,
                                    style = TextStyle(lineHeight = 36.sp)
                                )
                            }
                        },
                        description = "La plateforme ivoirienne qui vous montre tous les points de vente de gaz aux alentours.",
                    )
                    1 -> OnboardingPage(
                        imageRes = R.drawable.image2,
                        imageDesc = "Illustration de recherche rapide",
                        title = "Finis les longues recherches",
                        description = "En un clic retrouvez tous les points de vente de bouteilles de gaz autour de vous.",
                    )
                    2 -> OnboardingPage(
                        imageRes = R.drawable.image3,
                        imageDesc = "Illustration de livraison à domicile",
                        title = "Recevez vos bouteilles chez vous",
                        description = "Trouvez un point de vente, passez votre commande et recevez votre bouteille de gaz chez vous.",
                    )
                    3 -> OnboardingPage(
                        imageRes = R.drawable.image4,
                        imageDesc = "Illustration de commande rapide",
                        title = "Commencez à vous faire livrer vos bouteilles de gaz",
                        description = "Commandez en un clic et recevez votre bouteille chap chap",
                    )
                }
            }

            // ==========================================
            // --- SECTION 5 : PIED DE PAGE (INDICATEURS & BOUTON ACTION) ---
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicateurs de page (4 Dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) OnboardingIndicatorActiveWidth else OnboardingIndicatorInactiveWidth)
                                .height(OnboardingIndicatorHeight)
                                .clip(CircleShape)
                                .background(if (isSelected) YaDarkBlue else TextGrayBlue.copy(alpha = 0.3f))
                        )
                    }
                }

                // Bouton "Suivant" (pages 0, 1, 2) / "Commencer" (page 3)
                Button(
                    onClick = {
                        if (pagerState.currentPage < 3) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            showAuthSheet = true
                        }
                    },
                    shape = RoundedCornerShape(OnboardingButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonBg,
                        contentColor = ButtonText,
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(OnboardingButtonHeight)
                ) {
                    Text(
                        text = if (pagerState.currentPage < 3) "Suivant" else "Se connecter",
                        fontFamily = ComfortaaBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = ButtonText,
                        textAlign = TextAlign.Center,
                    )
                }

                // Bouton "Créer un compte" sur la dernière page
                if (pagerState.currentPage == 3) {
                    OutlinedButton(
                        onClick = onCreerCompte,
                        shape = RoundedCornerShape(OnboardingButtonCornerRadius),
                        border = androidx.compose.foundation.BorderStroke(2.dp, ButtonBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(OnboardingButtonHeight)
                    ) {
                        Text(
                            text = "Créer un compte",
                            fontFamily = ComfortaaBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = ButtonBg,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // ==========================================
        // --- SECTION 6 : FEUILLE MODALE D'AUTHENTIFICATION ANIMÉE ---
        // ==========================================
        AnimatedVisibility(
            visible = showAuthSheet,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 350)
            ) + fadeIn(animationSpec = tween(350)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)) // Fond sombre semi-transparent permettant de voir la présentation en arrière-plan
                    .clickable(onClick = { showAuthSheet = false }),
                contentAlignment = Alignment.BottomCenter
            ) {
                EcranAuthSheet(
                    onSuccess = {
                        showAuthSheet = false
                        onLoginSuccess()
                    },
                    onDismiss = { showAuthSheet = false },
                    onCreerCompte = {
                        showAuthSheet = false
                        onCreerCompte()
                    }
                )
            }
        }
    }
}

/**
 * Composant représentant une page individuelle dans le Pager d'Onboarding.
 */
@Composable
fun OnboardingPage(
    imageRes: Int,
    imageDesc: String,
    title: String = "",
    titleContent: (@Composable () -> Unit)? = null,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = imageDesc,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .heightIn(max = OnboardingIllustrationMaxHeight)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    text = title,
                    fontFamily = NunitoBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = TextGrayBlue,
                    textAlign = TextAlign.Center,
                    style = TextStyle(lineHeight = 32.sp),
                )
            }

            Text(
                text = description,
                fontFamily = ComfortaaBold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextGrayBlue,
                textAlign = TextAlign.Center,
                style = TextStyle(lineHeight = 22.sp),
                modifier = Modifier.fillMaxWidth(0.95f),
            )
        }
    }
}
