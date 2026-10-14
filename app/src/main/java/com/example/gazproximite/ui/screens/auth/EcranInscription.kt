package com.example.gazproximite.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
// --- ÉTAPES DE L'INSCRIPTION ---
// ==========================================
private enum class EtapeInscription {
    FORMULAIRE,      // Saisie des informations du client
    ENVOI_SMS,       // Firebase envoie le code
    CODE,            // Saisie du code reçu par SMS
    CREATION,        // Enregistrement du profil dans Firestore
    COMPTE_EXISTANT  // Ce numéro possède déjà un compte client
}

// Teinte du panneau, identique au panneau de connexion
private val CouleurPanneau = Color(0xFFC4BCD4)
private val CouleurBouton = Color(0xFF1E1E24)
private val CouleurErreur = Color(0xFFFF5252)

/**
 * [EcranInscription] - Page de création d'un compte client.
 *
 * 1. Le client saisit son nom, son prénom, son téléphone, sa commune et son e-mail.
 * 2. Firebase lui envoie un code par SMS.
 * 3. Le code validé, son profil est créé dans Firestore (collection "utilisateurs").
 */
@Composable
fun EcranInscription(
    onInscriptionReussie: () -> Unit = {},
    onSeConnecter: () -> Unit = {},
    onRetour: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Connexion Firebase par SMS et profils Firestore
    val authRepository = remember { ClientAuthRepository() }
    val clientRepository = remember { ClientRepository() }

    var etape by remember { mutableStateOf(EtapeInscription.FORMULAIRE) }

    // ==========================================
    // --- CHAMPS DU FORMULAIRE ---
    // ==========================================
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var commune by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var erreurFormulaire by remember { mutableStateOf<String?>(null) }

    // Code reçu par SMS
    var code by remember { mutableStateOf("") }
    var erreurCode by remember { mutableStateOf<String?>(null) }
    var verificationEnCours by remember { mutableStateOf(false) }

    // Profil déjà existant pour ce numéro
    var profilExistant by remember { mutableStateOf<Utilisateur?>(null) }

    val focusRequesters = remember { List(6) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus sur la première case du code
    LaunchedEffect(etape) {
        if (etape == EtapeInscription.CODE) {
            delay(150)
            try {
                focusRequesters[0].requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        }
    }

    // Numéro au format international attendu par Firebase
    val numeroInternational = "+225" + telephone.trim().replace(" ", "")

    // Création du profil Firestore une fois le numéro vérifié
    suspend fun creerCompte(uid: String) {
        // Si ce numéro a déjà un profil client, on ne l'écrase pas.
        val profil = clientRepository.recupererProfilClient(uid).getOrNull()
        if (profil != null && profil.role == "client") {
            profilExistant = profil
            etape = EtapeInscription.COMPTE_EXISTANT
            return
        }

        etape = EtapeInscription.CREATION
        clientRepository.creerProfilClient(
            uid = uid,
            nom = nom,
            prenom = prenom,
            adresse = commune,
            telephone = numeroInternational,
            email = email
        ).onSuccess {
            UserSessionManager.saveUserSession(
                context,
                Utilisateur(
                    uid = uid,
                    nom = nom.trim(),
                    prenom = prenom.trim(),
                    telephone = numeroInternational,
                    adresse = commune.trim(),
                    email = email.trim(),
                    role = "client"
                )
            )
            onInscriptionReussie()
        }.onFailure { e ->
            etape = EtapeInscription.FORMULAIRE
            erreurFormulaire = "Erreur Firestore : ${e.localizedMessage ?: "erreur inconnue"}"
        }
    }

    // Vérifie le formulaire puis demande l'envoi du SMS
    val envoyerCode: () -> Unit = envoi@{
        val chiffres = telephone.trim().replace(" ", "")
        erreurFormulaire = when {
            nom.isBlank() || prenom.isBlank() -> "Veuillez renseigner votre nom et votre prénom."
            chiffres.length != 10 -> "Le numéro de téléphone doit comporter exactement 10 chiffres."
            email.isNotBlank() && !email.contains("@") -> "L'adresse e-mail n'est pas valide."
            else -> null
        }
        if (erreurFormulaire != null) return@envoi

        val activity = context.trouverActivite()
        if (activity == null) {
            erreurFormulaire = "Impossible d'envoyer le SMS depuis cet écran."
            return@envoi
        }

        code = ""
        erreurCode = null
        etape = EtapeInscription.ENVOI_SMS

        authRepository.envoyerCodeSMS(
            activity = activity,
            telephone = numeroInternational,
            onCodeEnvoye = { etape = EtapeInscription.CODE },
            onConnexionAutomatique = { uid ->
                // Firebase a vérifié le numéro sans code à saisir.
                coroutineScope.launch { creerCompte(uid) }
            },
            onErreur = { detail ->
                etape = EtapeInscription.FORMULAIRE
                erreurFormulaire = "Erreur Firebase : $detail"
            }
        )
    }

    // Vérifie le code saisi auprès de Firebase
    val validerCode: () -> Unit = {
        erreurCode = null
        verificationEnCours = true
        coroutineScope.launch {
            val resultat = authRepository.verifierCodeSMS(code.trim())
            verificationEnCours = false
            resultat
                .onSuccess { uid -> creerCompte(uid) }
                .onFailure { e ->
                    erreurCode = "Code invalide ou expiré : ${e.localizedMessage ?: "vérifiez le code"}"
                }
        }
    }

    // ==========================================
    // --- STRUCTURE DE L'ÉCRAN ---
    // ==========================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
    ) {
        // --- En-tête : retour et logo ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRetour,
                enabled = etape == EtapeInscription.FORMULAIRE || etape == EtapeInscription.CODE,
                modifier = Modifier
                    .size(40.dp)
                    .background(AppBackground, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = YaDarkBlue
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_yagaz2),
                contentDescription = "Logo officiel YaGaz",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(40.dp)
            )
        }

        // --- Illustration et titre ---
        Image(
            painter = painterResource(id = R.drawable.image2),
            contentDescription = "Illustration de recherche de gaz",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 40.dp)
        )

        Text(
            text = "Créer votre compte",
            fontFamily = NunitoBold,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = YaDarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Rejoignez YaGaz pour trouver le gaz le plus proche de chez vous.",
            fontFamily = ComfortaaBold,
            fontSize = 14.sp,
            color = TextGrayBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Panneau du formulaire ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .background(CouleurPanneau)
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (etape) {

                // ==========================================
                // --- ÉTAPE 1 : FORMULAIRE ---
                // ==========================================
                EtapeInscription.FORMULAIRE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CustomAuthTextField(
                            value = nom,
                            onValueChange = { nom = it; erreurFormulaire = null },
                            placeholder = "Ex. : Koné",
                            label = "Nom*",
                            modifier = Modifier.weight(1f)
                        )
                        CustomAuthTextField(
                            value = prenom,
                            onValueChange = { prenom = it; erreurFormulaire = null },
                            placeholder = "Ex. : Awa",
                            label = "Prénom*",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Téléphone*",
                            fontFamily = ComfortaaBold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
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
                                Box(contentAlignment = Alignment.Center) {
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
                                value = telephone,
                                onValueChange = {
                                    telephone = it.filter { c -> c.isDigit() }.take(10)
                                    erreurFormulaire = null
                                },
                                placeholder = "07 00 00 00 00",
                                keyboardType = KeyboardType.Phone,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    CustomAuthTextField(
                        value = commune,
                        onValueChange = { commune = it },
                        placeholder = "Ex. : Cocody, Angré",
                        label = "Commune (facultatif)",
                        leadingIcon = Icons.Default.LocationOn
                    )

                    CustomAuthTextField(
                        value = email,
                        onValueChange = { email = it; erreurFormulaire = null },
                        placeholder = "Adresse e-mail",
                        label = "E-mail (facultatif)",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    if (erreurFormulaire != null) {
                        Text(
                            text = erreurFormulaire!!,
                            color = CouleurErreur,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    BoutonInscription(
                        texte = "Recevoir le code par SMS",
                        onClick = envoyerCode
                    )

                    Text(
                        text = "Déjà un compte ? Se connecter",
                        fontFamily = ComfortaaBold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSeConnecter)
                            .padding(vertical = 4.dp)
                    )
                }

                // ==========================================
                // --- ÉTAPES D'ATTENTE (SMS, FIRESTORE) ---
                // ==========================================
                EtapeInscription.ENVOI_SMS, EtapeInscription.CREATION -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(54.dp),
                        color = Color.White,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = if (etape == EtapeInscription.ENVOI_SMS) {
                            "Envoi du code par SMS…"
                        } else {
                            "Création de votre compte…"
                        },
                        fontFamily = ComfortaaBold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ==========================================
                // --- ÉTAPE 2 : CODE REÇU PAR SMS ---
                // ==========================================
                EtapeInscription.CODE -> {
                    Text(
                        text = "Un code à 6 chiffres a été envoyé par SMS au\n+225 $telephone",
                        fontFamily = ComfortaaBold,
                        fontSize = 13.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ChampCodeOtp(
                        code = code,
                        onCodeChange = {
                            code = it
                            erreurCode = null
                        },
                        focusRequesters = focusRequesters
                    )

                    if (erreurCode != null) {
                        Text(
                            text = erreurCode!!,
                            color = CouleurErreur,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "Renvoyer le code",
                        fontFamily = ComfortaaBold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clickable(onClick = envoyerCode)
                            .padding(vertical = 4.dp)
                    )

                    BoutonInscription(
                        texte = if (verificationEnCours) "Vérification en cours..." else "Valider et créer mon compte",
                        actif = code.length == 6 && !verificationEnCours,
                        onClick = validerCode
                    )

                    Text(
                        text = "← Modifier mes informations",
                        fontFamily = ComfortaaBold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clickable { etape = EtapeInscription.FORMULAIRE }
                            .padding(vertical = 4.dp)
                    )
                }

                // ==========================================
                // --- CAS : LE NUMÉRO A DÉJÀ UN COMPTE ---
                // ==========================================
                EtapeInscription.COMPTE_EXISTANT -> {
                    val profil = profilExistant
                    Text(
                        text = "Vous avez déjà un compte !",
                        fontFamily = NunitoBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Le numéro +225 $telephone est déjà associé au compte de " +
                                "${profil?.prenom.orEmpty()} ${profil?.nom.orEmpty()}. " +
                                "Vous êtes maintenant connecté.",
                        fontFamily = ComfortaaBold,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                    BoutonInscription(
                        texte = "Accéder à l'application",
                        onClick = {
                            profil?.let { UserSessionManager.saveUserSession(context, it) }
                            onInscriptionReussie()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Bouton principal sombre du panneau d'inscription.
 */
@Composable
private fun BoutonInscription(
    texte: String,
    onClick: () -> Unit,
    actif: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = actif,
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CouleurBouton,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(
            text = texte,
            fontFamily = ComfortaaBold,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
