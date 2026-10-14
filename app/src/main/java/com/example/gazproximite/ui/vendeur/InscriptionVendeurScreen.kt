package com.example.gazproximite.ui.vendeur

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.location.LocationRepository
import com.example.gazproximite.data.model.Utilisateur
import com.example.gazproximite.data.repository.VendeurRepository
import com.example.gazproximite.ui.composants.BandeauMessage
import com.example.gazproximite.ui.composants.ChampTexte
import com.example.gazproximite.ui.composants.TitreSection
import com.example.gazproximite.ui.ecrans.BoutonPrincipal
import com.example.gazproximite.ui.ecrans.GabaritAuth
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import java.util.Locale

// Création d'un compte vendeur et de son dépôt.
// La position du dépôt est celle du téléphone au moment de l'inscription :
// le vendeur doit donc s'inscrire depuis son dépôt.
@Composable
fun InscriptionVendeurScreen(
    context: Context,
    vendeurRepository: VendeurRepository,
    onRetour: () -> Unit,
    onInscriptionReussie: (Utilisateur) -> Unit
) {
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var nomDepot by remember { mutableStateOf("") }
    var commune by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }

    // Position GPS du dépôt.
    var position by remember { mutableStateOf<GeoPoint?>(null) }
    var recherchePosition by remember { mutableStateOf(false) }

    var chargement by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val locationRepository = remember { LocationRepository(context) }

    // Récupère la position actuelle du téléphone.
    fun localiserDepot() {
        if (!locationRepository.localisationActivee()) {
            message = "Active la localisation (GPS) du téléphone, puis réessaie."
            erreur = true
            return
        }

        recherchePosition = true
        scope.launch {
            val resultat = locationRepository.recupererPosition()
            recherchePosition = false
            if (resultat == null) {
                message = "Position introuvable. Place-toi à l'extérieur et réessaie."
                erreur = true
            } else {
                position = GeoPoint(resultat.latitude, resultat.longitude)
                message = ""
            }
        }
    }

    val demandePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val autorisee = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (autorisee) {
            localiserDepot()
        } else {
            message = "La localisation est nécessaire pour placer ton dépôt sur la carte."
            erreur = true
        }
    }

    GabaritAuth(
        titre = "Créer mon dépôt",
        sousTitre = "Inscris ton dépôt pour qu'il apparaisse sur la carte des clients.",
        icone = Icons.Filled.AddBusiness,
        onRetour = onRetour,
        retourActif = !chargement
    ) {
        TitreSection(titre = "Le responsable")

        ChampTexte(
            valeur = prenom,
            onValeurChange = { prenom = it },
            libelle = "Prénom",
            icone = Icons.Filled.Person,
            actif = !chargement,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        ChampTexte(
            valeur = nom,
            onValeurChange = { nom = it },
            libelle = "Nom",
            icone = Icons.Filled.Badge,
            actif = !chargement,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        ChampTexte(
            valeur = email,
            onValeurChange = { email = it },
            libelle = "Adresse e-mail",
            icone = Icons.Filled.Email,
            exemple = "vendeur@exemple.ci",
            actif = !chargement,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        ChampMotDePasse(
            valeur = motDePasse,
            onValeurChange = { motDePasse = it },
            libelle = "Mot de passe (6 caractères minimum)",
            actif = !chargement
        )

        Spacer(modifier = Modifier.height(4.dp))
        TitreSection(titre = "Le dépôt")

        ChampTexte(
            valeur = nomDepot,
            onValeurChange = { nomDepot = it },
            libelle = "Nom du dépôt",
            icone = Icons.Filled.Storefront,
            exemple = "Ex. : Dépôt Angré 8e Tranche",
            actif = !chargement,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        ChampTexte(
            valeur = commune,
            onValeurChange = { commune = it },
            libelle = "Commune",
            icone = Icons.Filled.LocationCity,
            exemple = "Ex. : Cocody",
            actif = !chargement,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        ChampTexte(
            valeur = telephone,
            onValeurChange = { telephone = it },
            libelle = "Téléphone du dépôt",
            icone = Icons.Filled.Phone,
            exemple = "+225 07 00 00 00 00",
            actif = !chargement,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        // Bouton de localisation du dépôt.
        OutlinedButton(
            onClick = {
                if (locationRepository.localisationAutorisee()) {
                    localiserDepot()
                } else {
                    demandePermission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            enabled = !chargement && !recherchePosition,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                when {
                    recherchePosition -> "Recherche de la position..."
                    position != null -> "Position enregistrée ✓ (refaire)"
                    else -> "Utiliser ma position actuelle"
                }
            )
        }

        position?.let { point ->
            Text(
                text = String.format(
                    Locale.FRANCE,
                    "Latitude %.5f · Longitude %.5f",
                    point.latitude,
                    point.longitude
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BoutonPrincipal(
            texte = "Créer mon dépôt",
            chargement = chargement,
            onClick = {
                val point = position
                message = when {
                    prenom.isBlank() || nom.isBlank() -> "Renseigne ton prénom et ton nom."
                    !email.contains("@") -> "L'adresse e-mail n'est pas valide."
                    motDePasse.length < 6 -> "Le mot de passe doit contenir au moins 6 caractères."
                    nomDepot.isBlank() || commune.isBlank() -> "Renseigne le nom et la commune du dépôt."
                    telephone.isBlank() -> "Renseigne le téléphone du dépôt."
                    point == null -> "Appuie sur « Utiliser ma position actuelle » pour placer le dépôt."
                    else -> ""
                }
                erreur = true
                if (message.isNotBlank() || point == null) return@BoutonPrincipal

                chargement = true
                scope.launch {
                    vendeurRepository.inscrire(
                        email = email,
                        motDePasse = motDePasse,
                        nom = nom,
                        prenom = prenom,
                        nomDepot = nomDepot,
                        commune = commune,
                        telephone = telephone,
                        latitude = point.latitude,
                        longitude = point.longitude
                    ).onSuccess { vendeur ->
                        chargement = false
                        onInscriptionReussie(vendeur)
                    }.onFailure { e ->
                        chargement = false
                        message = "Inscription impossible : ${e.localizedMessage ?: "erreur inconnue"}"
                        erreur = true
                    }
                }
            }
        )

        if (message.isNotBlank()) {
            BandeauMessage(message = message, erreur = erreur)
        }
    }
}
