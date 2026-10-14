package com.example.gazproximite

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.session.UserSessionManager
import com.example.gazproximite.ui.etat.rememberDepotsProches
import com.example.gazproximite.ui.screens.auth.EcranInscription
import com.example.gazproximite.ui.screens.depots.EcranDepotsList
import com.example.gazproximite.ui.screens.details.EcranDetails
import com.example.gazproximite.ui.screens.favorites.EcranFavorites
import com.example.gazproximite.ui.screens.home.EcranHome
import com.example.gazproximite.ui.screens.onboarding.EcranOnboarding
import com.example.gazproximite.ui.screens.profile.EcranProfile
import com.example.gazproximite.ui.screens.splash.Ecran1Splash
import com.example.gazproximite.ui.screens.splash.Ecran2Presentation
import com.example.gazproximite.ui.theme.YaGazTheme

enum class AppScreen {
    SPLASH_1,
    SPLASH_2,
    ONBOARDING,
    INSCRIPTION,
    HOME,
    DEPOTS_LIST,
    FAVORITES,
    PROFILE,
    DETAILS
}

// Écrans de l'espace client (après la connexion).
private val ecransConnectes = setOf(
    AppScreen.HOME,
    AppScreen.DEPOTS_LIST,
    AppScreen.FAVORITES,
    AppScreen.PROFILE,
    AppScreen.DETAILS
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )
        setContent {
            YaGazTheme {
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf(AppScreen.SPLASH_1) }

                // Écran d'où l'on vient avant d'ouvrir le détail d'un dépôt
                var ecranPrecedent by remember { mutableStateOf(AppScreen.HOME) }

                // Vrai quand on revient de l'inscription pour se connecter
                var ouvrirConnexion by remember { mutableStateOf(false) }

                // Dépôt ouvert dans l'écran de détail
                var depotSelectionne by remember { mutableStateOf<DepotAvecDistance?>(null) }

                // Préférences gardées sur le téléphone
                var favoris by remember { mutableStateOf(UserSessionManager.getFavoris(context)) }
                var modeDemo by remember { mutableStateOf(UserSessionManager.isModeDemo(context)) }

                // Dépôts réels (Firestore) chargés une seule fois pour tous les écrans,
                // seulement quand le client est dans son espace.
                val dansEspaceClient = currentScreen in ecransConnectes
                val etatDepots = if (dansEspaceClient) {
                    rememberDepotsProches(context, inclureSimulation = modeDemo)
                } else {
                    null
                }

                val handleTabSelected: (Int) -> Unit = { tabIndex ->
                    currentScreen = when (tabIndex) {
                        0 -> AppScreen.HOME
                        1 -> AppScreen.DEPOTS_LIST
                        2 -> AppScreen.FAVORITES
                        3 -> AppScreen.PROFILE
                        else -> AppScreen.HOME
                    }
                }

                val ouvrirDetail: (DepotAvecDistance) -> Unit = { depot ->
                    depotSelectionne = depot
                    ecranPrecedent = currentScreen
                    currentScreen = AppScreen.DETAILS
                }

                // Le bouton "retour" du téléphone revient à l'écran précédent.
                BackHandler(enabled = currentScreen == AppScreen.INSCRIPTION) {
                    ouvrirConnexion = false
                    currentScreen = AppScreen.ONBOARDING
                }
                BackHandler(enabled = currentScreen == AppScreen.DETAILS) {
                    currentScreen = ecranPrecedent
                }
                BackHandler(
                    enabled = currentScreen in setOf(
                        AppScreen.DEPOTS_LIST,
                        AppScreen.FAVORITES,
                        AppScreen.PROFILE
                    )
                ) {
                    currentScreen = AppScreen.HOME
                }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + slideInHorizontally(animationSpec = tween(400)) { width -> width / 4 })
                            .togetherWith(fadeOut(animationSpec = tween(400)) + slideOutHorizontally(animationSpec = tween(400)) { width -> -width / 4 })
                    },
                    label = "screenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.SPLASH_1 -> {
                            Ecran1Splash(
                                onFinish = {
                                    // Si le client est déjà connecté (session Firebase active),
                                    // on contourne les écrans de présentation.
                                    currentScreen = if (UserSessionManager.isLoggedIn(context)) {
                                        AppScreen.HOME
                                    } else {
                                        AppScreen.SPLASH_2
                                    }
                                }
                            )
                        }
                        AppScreen.SPLASH_2 -> {
                            Ecran2Presentation(
                                onFinish = { currentScreen = AppScreen.ONBOARDING }
                            )
                        }
                        AppScreen.ONBOARDING -> {
                            EcranOnboarding(
                                onLoginSuccess = { currentScreen = AppScreen.HOME },
                                onCreerCompte = { currentScreen = AppScreen.INSCRIPTION },
                                ouvrirConnexion = ouvrirConnexion
                            )
                        }
                        AppScreen.INSCRIPTION -> {
                            EcranInscription(
                                onInscriptionReussie = { currentScreen = AppScreen.HOME },
                                onSeConnecter = {
                                    ouvrirConnexion = true
                                    currentScreen = AppScreen.ONBOARDING
                                },
                                onRetour = {
                                    ouvrirConnexion = false
                                    currentScreen = AppScreen.ONBOARDING
                                }
                            )
                        }
                        AppScreen.HOME -> {
                            etatDepots?.let { etat ->
                                EcranHome(
                                    etat = etat,
                                    onDepotClick = ouvrirDetail,
                                    onTabSelected = handleTabSelected
                                )
                            }
                        }
                        AppScreen.DEPOTS_LIST -> {
                            etatDepots?.let { etat ->
                                EcranDepotsList(
                                    etat = etat,
                                    onDepotClick = ouvrirDetail,
                                    onTabSelected = handleTabSelected
                                )
                            }
                        }
                        AppScreen.FAVORITES -> {
                            etatDepots?.let { etat ->
                                EcranFavorites(
                                    etat = etat,
                                    favoris = favoris,
                                    onDepotClick = ouvrirDetail,
                                    onTabSelected = handleTabSelected
                                )
                            }
                        }
                        AppScreen.PROFILE -> {
                            EcranProfile(
                                modeDemo = modeDemo,
                                onModeDemoChange = { actif ->
                                    modeDemo = actif
                                    UserSessionManager.setModeDemo(context, actif)
                                },
                                onTabSelected = handleTabSelected,
                                onLogout = { currentScreen = AppScreen.ONBOARDING }
                            )
                        }
                        AppScreen.DETAILS -> {
                            val depot = depotSelectionne
                            if (depot == null) {
                                // Aucun dépôt choisi : retour à l'accueil.
                                LaunchedEffect(Unit) { currentScreen = AppScreen.HOME }
                            } else {
                                EcranDetails(
                                    depotAvecDistance = depot,
                                    estFavori = depot.depot.id in favoris,
                                    onFavoriClick = {
                                        favoris = UserSessionManager.basculerFavori(context, depot.depot.id)
                                    },
                                    onBackClick = { currentScreen = ecranPrecedent },
                                    onTabSelected = handleTabSelected
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
