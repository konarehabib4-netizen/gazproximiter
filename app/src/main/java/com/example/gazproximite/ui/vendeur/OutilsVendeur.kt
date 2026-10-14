package com.example.gazproximite.ui.vendeur

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gazproximite.data.model.Depot
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque
import com.example.gazproximite.data.model.Vente
import com.example.gazproximite.data.model.cleStock
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// =========================================================
// CALCULS SUR LE STOCK ET LES VENTES
// =========================================================

// Nombre de bouteilles en stock pour une marque et un format.
fun quantiteEnStock(depot: Depot, marque: Marque, format: Format): Long =
    depot.quantites[cleStock(marque, format)] ?: 0L

// Prix de vente d'une bouteille (0 si non renseigné).
fun prixBouteille(depot: Depot, marque: Marque, format: Format): Long =
    depot.prix[cleStock(marque, format)] ?: 0L

// Total des bouteilles en stock, toutes marques confondues.
fun totalBouteilles(depot: Depot): Long = depot.quantites.values.sum()

// Seuil en dessous duquel le stock est jugé faible.
const val SEUIL_STOCK_FAIBLE = 3L

// Ventes enregistrées aujourd'hui.
fun ventesDuJour(ventes: List<Vente>): List<Vente> {
    val debutJournee = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // Une vente encore en cours de synchronisation (date null) est d'aujourd'hui.
    return ventes.filter { vente -> vente.date?.toDate()?.after(debutJournee) ?: true }
}

// =========================================================
// MISE EN FORME
// =========================================================

// 4500 -> "4 500 F CFA"
fun formaterPrix(montant: Long): String =
    NumberFormat.getIntegerInstance(Locale.FRANCE).format(montant) + " F CFA"

// Date et heure d'une vente, ex. "14/10 à 15:42".
fun formaterDateVente(vente: Vente): String {
    val date = vente.date?.toDate() ?: return "à l'instant"
    return SimpleDateFormat("dd/MM 'à' HH:mm", Locale.FRANCE).format(date)
}

// Nom lisible d'une vente, ex. "B12 Petroci".
fun libelleVente(vente: Vente): String =
    "${vente.formatEnum?.name ?: vente.format.uppercase()} ${vente.marqueEnum?.libelle ?: vente.marque}"

// =========================================================
// COMPOSANTS
// =========================================================

// Champ mot de passe avec bouton pour afficher ou masquer la saisie.
@Composable
fun ChampMotDePasse(
    valeur: String,
    onValeurChange: (String) -> Unit,
    libelle: String = "Mot de passe",
    actif: Boolean = true
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valeur,
        onValueChange = onValeurChange,
        label = { Text(libelle) },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Masquer le mot de passe" else "Afficher le mot de passe"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        enabled = actif,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// Carte blanche arrondie qui regroupe une section de l'espace vendeur.
@Composable
fun CarteVendeur(
    modifier: Modifier = Modifier,
    contenu: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = contenu
        )
    }
}
