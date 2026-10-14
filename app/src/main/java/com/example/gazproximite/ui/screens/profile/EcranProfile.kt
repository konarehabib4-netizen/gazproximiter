package com.example.gazproximite.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gazproximite.R
import com.example.gazproximite.data.repository.ClientRepository
import com.example.gazproximite.data.session.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.example.gazproximite.ui.screens.home.CustomBottomNavBar
import com.example.gazproximite.ui.theme.AppBackground
import com.example.gazproximite.ui.theme.YaDarkBlue

/**
 * [EcranProfile] - Écran de profil et paramètres de l'utilisateur.
 * Affiche la carte utilisateur avec les données de session du client connecté,
 * les options du compte, les préférences et le bouton de déconnexion.
 */
@Composable
fun EcranProfile(
    modeDemo: Boolean = false,
    onModeDemoChange: (Boolean) -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    // Copie locale affichée tout de suite, puis mise à jour depuis Firestore
    var userProfile by remember { mutableStateOf(UserSessionManager.getUserProfile(context)) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        ClientRepository().recupererProfilClient(uid).onSuccess { profil ->
            if (profil != null) {
                UserSessionManager.saveUserSession(context, profil)
                userProfile = UserSessionManager.getUserProfile(context)
            }
        }
    }

    // ==========================================
    // --- SECTION 1 : ÉTATS DES PRÉFÉRENCES ---
    // ==========================================
    var darkModeEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    // Nom complet dynamiquement extrait
    val fullName = remember(userProfile) {
        val name = "${userProfile.prenom} ${userProfile.nom}".trim()
        if (name.isNotBlank()) name else "Client YaGaz"
    }

    val displayEmail = userProfile.email.ifBlank { "E-mail non renseigné" }
    val displayContact = userProfile.contact.ifBlank { "Téléphone non renseigné" }
    val displayLocation = if (userProfile.commune.isNotBlank()) {
        "${userProfile.ville}, ${userProfile.commune}"
    } else {
        "${userProfile.ville}, Côte d'Ivoire"
    }

    // ==========================================
    // --- SECTION 2 : CONTENEUR PRINCIPAL ---
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // En-tête
            Surface(
                color = AppBackground,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_yagaz2),
                        contentDescription = "Logo officiel YaGaz",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.height(40.dp)
                    )
                }
            }

            // Contenu scrollable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 120.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Mon profil",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = YaDarkBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Carte Utilisateur Principale
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE0E0E0),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.image5),
                                contentDescription = "Avatar de l'utilisateur",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = fullName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YaDarkBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Compte vérifié",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = displayContact,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            Text(
                                text = displayEmail,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = YaDarkBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = displayLocation,
                                    fontSize = 12.sp,
                                    color = YaDarkBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section "Mon compte"
                Text(
                    text = "Mon compte",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = YaDarkBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ProfileMenuItem(
                            icon = Icons.Filled.Person,
                            title = "Informations personnelles",
                            onClick = {}
                        )
                        ProfileMenuItem(
                            icon = Icons.Filled.LocationOn,
                            title = "Mes adresses de livraison",
                            onClick = {}
                        )
                        ProfileMenuItem(
                            icon = Icons.Filled.ShoppingCart,
                            title = "Historique des commandes",
                            onClick = {}
                        )
                        ProfileMenuItem(
                            icon = Icons.Filled.CreditCard,
                            title = "Moyens de paiement",
                            onClick = {}
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section "Mes paramètres"
                Text(
                    text = "Mes paramètres",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = YaDarkBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ProfileSwitchMenuItem(
                            icon = Icons.Filled.Science,
                            title = "Mode démonstration (dépôts fictifs)",
                            checked = modeDemo,
                            onCheckedChange = onModeDemoChange
                        )
                        ProfileSwitchMenuItem(
                            icon = Icons.Filled.Notifications,
                            title = "Notifications",
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                        ProfileSwitchMenuItem(
                            icon = Icons.Filled.DarkMode,
                            title = "Mode sombre",
                            checked = darkModeEnabled,
                            onCheckedChange = { darkModeEnabled = it }
                        )
                        ProfileMenuItem(
                            icon = Icons.Filled.Security,
                            title = "Sécurité & Confidentialité",
                            onClick = {}
                        )
                        ProfileMenuItem(
                            icon = Icons.Filled.Language,
                            title = "Langue (Français)",
                            onClick = {}
                        )
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.Help,
                            title = "Aide & Support",
                            onClick = {}
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bouton de Déconnexion
                OutlinedButton(
                    onClick = {
                        UserSessionManager.logout(context)
                        onLogout()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Icône de déconnexion",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Se déconnecter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Barre de navigation flottante
        CustomBottomNavBar(
            selectedTab = 3,
            onTabSelected = onTabSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
        )
    }
}

/**
 * Composant de ligne de menu standard avec icône, titre et flèche de navigation.
 */
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = YaDarkBlue,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E1E24)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Naviguer vers l'option",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Composant de ligne de menu avec commutateur (Switch).
 */
@Composable
fun ProfileSwitchMenuItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = YaDarkBlue,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E1E24)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = YaDarkBlue
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun EcranProfilePreview() {
    EcranProfile()
}
