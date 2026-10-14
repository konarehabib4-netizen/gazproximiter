package com.example.gazproximite.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gazproximite.R
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.repository.ClientAuthRepository
import com.example.gazproximite.data.repository.ClientRepository
import com.example.gazproximite.data.session.UserSessionManager
import com.example.gazproximite.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// --- PROPRIÉTÉS ET DIMENSIONS LOCALES ---
// ==========================================
private val AuthHorizontalPadding: Dp = 24.dp
private val AuthBannerMaxHeight: Dp = 240.dp
private val AuthSheetCornerRadius: Dp = 36.dp
private val AuthButtonHeight: Dp = 54.dp
private val AuthButtonCornerRadius: Dp = 25.dp

// Retrouve l'activité Android (nécessaire à Firebase pour l'envoi du SMS).
internal fun Context.trouverActivite(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.trouverActivite()
    else -> null
}

/**
 * [EcranAuth] - Écran d'authentification plein écran.
 */
@Composable
fun EcranAuth(
    onSuccess: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // En-tête avec Logo et Illustration
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(AuthHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-30).dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_yagaz2),
                    contentDescription = "Logo officiel YaGaz",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(110.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Image(
                painter = painterResource(id = R.drawable.image3),
                contentDescription = "Illustration de livraison de gaz",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .heightIn(max = AuthBannerMaxHeight)
            )
        }

        EcranAuthSheet(
            onSuccess = onSuccess,
            onDismiss = onDismiss,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * [EcranAuthSheet] - Panneau/Modal coulissant d'authentification et d'inscription par Téléphone et OTP.
 */
@Composable
fun EcranAuthSheet(
    modifier: Modifier = Modifier,
    onSuccess: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onCreerCompte: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Connexion Firebase par SMS et profils Firestore
    val authRepository = remember { ClientAuthRepository() }
    val clientRepository = remember { ClientRepository() }

    // États du flux d'authentification :
    // 1 = numéro, 2 = envoi du SMS, 3 = code, 4 = compte existant, 5 = inscription
    var authStep by remember { mutableIntStateOf(1) }

    var phoneNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    var otpCode by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var isVerifyingOtp by remember { mutableStateOf(false) }

    // Focus Requesters pour les 6 champs de saisie de texte indépendants
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus sur la première case OTP dès l'arrivée à l'étape 3
    LaunchedEffect(authStep) {
        if (authStep == 3) {
            delay(150)
            try {
                focusRequesters[0].requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        }
    }

    // UID Firebase du client une fois son numéro vérifié
    var uidVerifie by remember { mutableStateOf<String?>(null) }

    // Profil Firestore trouvé pour ce numéro (compte existant)
    var profilTrouve by remember { mutableStateOf<Utilisateur?>(null) }

    // Champs du formulaire d'inscription (Cas B)
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var emailReg by remember { mutableStateOf("") }

    var formError by remember { mutableStateOf<String?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }

    // Numéro au format international attendu par Firebase
    val numeroInternational = "+225" + phoneNumber.trim().replace(" ", "")

    // Après la vérification du numéro : compte existant ou inscription ?
    suspend fun apresVerification(uid: String) {
        uidVerifie = uid
        val profil = clientRepository.recupererProfilClient(uid).getOrNull()

        if (profil != null && profil.role == "client") {
            profilTrouve = profil
            authStep = 4 // Cas A : Bienvenue
        } else {
            authStep = 5 // Cas B : Formulaire d'inscription
        }
    }

    // 1. Envoi du code SMS par Firebase
    val handlePhoneSubmit: () -> Unit = handle@{
        val cleanPhone = phoneNumber.trim().replace(" ", "")
        if (cleanPhone.length < 10) {
            phoneError = "Le numéro de téléphone doit comporter exactement 10 chiffres"
            return@handle
        }

        val activity = context.trouverActivite()
        if (activity == null) {
            phoneError = "Impossible d'envoyer le SMS depuis cet écran."
            return@handle
        }

        phoneError = null
        otpCode = ""
        authStep = 2 // Chargement

        authRepository.envoyerCodeSMS(
            activity = activity,
            telephone = numeroInternational,
            onCodeEnvoye = {
                authStep = 3 // Étape OTP
            },
            onConnexionAutomatique = { uid ->
                // Firebase a vérifié le numéro sans code à saisir.
                coroutineScope.launch { apresVerification(uid) }
            },
            onErreur = { detail ->
                authStep = 1
                phoneError = "Erreur Firebase : $detail"
            }
        )
    }

    // 2. Vérification du code reçu par SMS
    val handleVerifyOtp: () -> Unit = {
        otpError = null
        isVerifyingOtp = true
        coroutineScope.launch {
            val resultat = authRepository.verifierCodeSMS(otpCode.trim())
            isVerifyingOtp = false

            resultat
                .onSuccess { uid -> apresVerification(uid) }
                .onFailure { e ->
                    otpError = "Code invalide ou expiré : ${e.localizedMessage ?: "vérifie le code"}"
                }
        }
    }

    // Compte existant : on garde une copie locale du profil Firestore
    val handleExistingUserFinish: () -> Unit = {
        profilTrouve?.let { UserSessionManager.saveUserSession(context, it) }
        onSuccess()
    }

    // Nouveau compte : création du profil dans Firestore
    val handleRegisterFinish: () -> Unit = handle@{
        val uid = uidVerifie
        if (nom.isBlank() || prenom.isBlank()) {
            formError = "Veuillez renseigner votre nom et votre prénom"
            return@handle
        }
        if (uid == null) {
            formError = "Numéro non vérifié. Recommencez la connexion."
            return@handle
        }

        formError = null
        isSavingProfile = true
        coroutineScope.launch {
            clientRepository.creerProfilClient(
                uid = uid,
                nom = nom,
                prenom = prenom,
                adresse = "",
                telephone = numeroInternational,
                email = emailReg
            ).onSuccess {
                UserSessionManager.saveUserSession(
                    context,
                    Utilisateur(
                        uid = uid,
                        nom = nom.trim(),
                        prenom = prenom.trim(),
                        telephone = numeroInternational,
                        email = emailReg.trim(),
                        role = "client"
                    )
                )
                isSavingProfile = false
                onSuccess()
            }.onFailure { e ->
                isSavingProfile = false
                formError = "Erreur Firestore : ${e.localizedMessage ?: "erreur inconnue"}"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = AuthSheetCornerRadius,
                    topEnd = AuthSheetCornerRadius
                )
            )
            .background(Color(0xFFC4BCD4)) // Teinte violette/grise douce signature
            .clickable(enabled = false) {}
            .padding(AuthHorizontalPadding)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // En-tête Modal : Titre dynamique
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (authStep) {
                        1 -> "Connexion"
                        2 -> "Envoi en cours"
                        3 -> "Vérification du numéro"
                        4 -> "Bienvenue !"
                        else -> "Créer votre compte"
                    },
                    fontFamily = NunitoBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Fermer le panneau",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ==========================================
            // --- ÉTAPE 1 : SAISIE DU NUMÉRO DE TÉLÉPHONE ---
            // ==========================================
            if (authStep == 1) {
                Text(
                    text = "Entrez votre numéro de téléphone pour continuer sur YaGaz.",
                    fontFamily = ComfortaaBold,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        modifier = Modifier
                            .height(52.dp)
                            .width(85.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "🇨🇮 +225",
                                fontFamily = ComfortaaBold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = YaDarkBlue
                            )
                        }
                    }

                    CustomAuthTextField(
                        value = phoneNumber,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() }.take(10)
                            phoneNumber = filtered
                            phoneError = null
                        },
                        placeholder = "07 00 00 00 00",
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (phoneError != null) {
                    Text(
                        text = phoneError!!,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { handlePhoneSubmit() },
                    shape = RoundedCornerShape(AuthButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E24),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AuthButtonHeight)
                ) {
                    Text(
                        text = "Continuer",
                        fontFamily = ComfortaaBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                // Lien vers la page d'inscription
                Text(
                    text = "Pas encore de compte ? Créer un compte",
                    fontFamily = ComfortaaBold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreerCompte)
                        .padding(vertical = 6.dp)
                )
            }
            // ==========================================
            // --- ÉTAPE 2 : RECHERCHE DU COMPTE (CHARGEMENT) ---
            // ==========================================
            else if (authStep == 2) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(54.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Envoi du code par SMS…",
                    fontFamily = ComfortaaBold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Veuillez patienter, vous allez recevoir un code à 6 chiffres.",
                    fontFamily = ComfortaaBold,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            // ==========================================
            // --- ÉTAPE 3 : VÉRIFICATION OTP (6 CHAMPS DE TEXTE INDÉPENDANTS ET RÉACTIFS) ---
            // ==========================================
            else if (authStep == 3) {
                Text(
                    text = "Un code à 6 chiffres a été envoyé par SMS au\n+225 $phoneNumber",
                    fontFamily = ComfortaaBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 6 champs de saisie du code reçu par SMS
                ChampCodeOtp(
                    code = otpCode,
                    onCodeChange = {
                        otpCode = it
                        otpError = null
                    },
                    focusRequesters = focusRequesters
                )

                if (otpError != null) {
                    Text(
                        text = otpError!!,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(1.dp))

                    Text(
                        text = "Renvoyer le code",
                        fontFamily = ComfortaaBold,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { handlePhoneSubmit() }
                            .padding(vertical = 4.dp)
                    )
                }

                Button(
                    onClick = { handleVerifyOtp() },
                    enabled = otpCode.length == 6 && !isVerifyingOtp,
                    shape = RoundedCornerShape(AuthButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E24),
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AuthButtonHeight)
                ) {
                    if (isVerifyingOtp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Vérification en cours...",
                            fontFamily = ComfortaaBold,
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = "Vérifier",
                            fontFamily = ComfortaaBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Text(
                    text = "← Modifier le numéro",
                    fontFamily = ComfortaaBold,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { authStep = 1 }
                        .padding(vertical = 4.dp)
                )
            }
            // ==========================================
            // --- ÉTAPE 4 : CAS A (COMPTE EXISTANT - BIENVENUE) ---
            // ==========================================
            else if (authStep == 4) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ravi de vous revoir, ${profilTrouve?.prenom.orEmpty()} !",
                    fontFamily = NunitoBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Votre compte a été identifié avec succès. Vous pouvez accéder directement à votre espace YaGaz.",
                    fontFamily = ComfortaaBold,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "👤 Nom : ${profilTrouve?.nom.orEmpty()} ${profilTrouve?.prenom.orEmpty()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = YaDarkBlue)
                        Text(text = "📱 Numéro : +225 $phoneNumber", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = YaDarkBlue)
                        if (!profilTrouve?.email.isNullOrBlank()) {
                            Text(text = "✉️ E-mail : ${profilTrouve?.email}", fontSize = 13.sp, color = Color.Gray)
                        }
                        Text(text = "✨ Statut : Compte vérifié", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { handleExistingUserFinish() },
                    shape = RoundedCornerShape(AuthButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E24),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AuthButtonHeight)
                ) {
                    Text(
                        text = "Accéder à l'application",
                        fontFamily = ComfortaaBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            // ==========================================
            // --- ÉTAPE 5 : CAS B (NOUVEAU COMPTE - FORMULAIRE) ---
            // ==========================================
            else if (authStep == 5) {
                Text(
                    text = "Numéro vérifié ! Veuillez renseigner votre nom et prénom pour créer votre compte.",
                    fontFamily = ComfortaaBold,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomAuthTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        placeholder = "Nom*",
                        label = "Nom*",
                        modifier = Modifier.weight(1f)
                    )

                    CustomAuthTextField(
                        value = prenom,
                        onValueChange = { prenom = it },
                        placeholder = "Prénom*",
                        label = "Prénom*",
                        modifier = Modifier.weight(1f)
                    )
                }

                CustomAuthTextField(
                    value = emailReg,
                    onValueChange = { emailReg = it },
                    placeholder = "Adresse e-mail (facultatif)",
                    label = "E-mail (facultatif)",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )

                if (formError != null) {
                    Text(
                        text = formError!!,
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { handleRegisterFinish() },
                    enabled = !isSavingProfile,
                    shape = RoundedCornerShape(AuthButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E24),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AuthButtonHeight)
                ) {
                    Text(
                        text = if (isSavingProfile) "Création du compte..." else "Créer mon compte",
                        fontFamily = ComfortaaBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Composant de champ de saisie personnalisé pour l'authentification.
 */
@Composable
fun CustomAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontFamily = ComfortaaBold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = ComfortaaBold,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = YaDarkBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = YaDarkBlue,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = Color(0xFFFF5252),
                focusedTextColor = Color(0xFF1E1E24),
                unfocusedTextColor = Color(0xFF1E1E24)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
        )
    }
}
