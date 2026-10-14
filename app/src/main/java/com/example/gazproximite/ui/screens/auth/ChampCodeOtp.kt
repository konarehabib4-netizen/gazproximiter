package com.example.gazproximite.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gazproximite.ui.theme.YaDarkBlue

/**
 * 6 champs de saisie indépendants pour le code reçu par SMS.
 * Gère la saisie chiffre par chiffre, le collage d'un code complet et l'effacement.
 */
@Composable
fun ChampCodeOtp(
    code: String,
    onCodeChange: (String) -> Unit,
    focusRequesters: List<FocusRequester>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val digit = if (index < code.length) code[index].toString() else ""
            val isFilled = digit.isNotEmpty()
            val isFocused = index == code.length || (code.length == 6 && index == 5)
            val isActive = isFilled || isFocused

            OutlinedTextField(
                value = digit,
                onValueChange = { value ->
                    val digits = value.filter { it.isDigit() }
                    if (digits.length > 1) {
                        // Collage d'un code complet de 6 chiffres
                        val nouveau = digits.take(6)
                        onCodeChange(nouveau)
                        focusRequesters[minOf(nouveau.length, 5)].requestFocus()
                    } else if (digits.length == 1) {
                        // Saisie d'un chiffre unique
                        val chars = code.padEnd(6, ' ').toCharArray()
                        chars[index] = digits[0]
                        onCodeChange(chars.concatToString().trimEnd())
                        if (index < 5) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    } else {
                        // Effacement (Backspace)
                        val chars = code.padEnd(6, ' ').toCharArray()
                        chars[index] = ' '
                        onCodeChange(chars.concatToString().trimEnd())
                        if (index > 0 && value.isEmpty()) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isActive) YaDarkBlue.copy(alpha = 0.08f) else Color.White,
                    unfocusedContainerColor = if (isActive) YaDarkBlue.copy(alpha = 0.08f) else Color.White,
                    disabledContainerColor = if (isActive) YaDarkBlue.copy(alpha = 0.08f) else Color.White,
                    focusedBorderColor = YaDarkBlue,
                    unfocusedBorderColor = if (isActive) YaDarkBlue else Color.LightGray,
                    focusedTextColor = YaDarkBlue,
                    unfocusedTextColor = YaDarkBlue
                ),
                modifier = Modifier
                    .size(44.dp, 52.dp)
                    .focusRequester(focusRequesters[index])
            )
        }
    }
}
