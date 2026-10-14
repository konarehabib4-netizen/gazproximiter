package com.example.gazproximite.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * [YaGazPalette] centralise toutes les couleurs et palettes de l'application.
 * Vous pouvez modifier n'importe quelle couleur ici pour changer l'apparence globale du projet.
 */
object YaGazPalette {
    // --- Palette de base YaGaz (Tons bleus signature) ---
    val OxfordByNight   = Color(0xFF061C2C) // Très sombre / Proche du noir
    val NatoBlue        = Color(0xFF193347) // Bleu marine profond
    val Nato2           = Color(0xFF3B5264) // Bleu intermédiaire
    val BlueFedora      = Color(0xFF334B60) // Bleu moyen élégant
    val PrairieDenim    = Color(0xFF4F6478) // Bleu denim poudré
    val BluePrince      = Color(0xFF6C7F91) // Bleu gris doux
    val Foghorn         = Color(0xFF91A1B2) // Gris bleuté clair
    val PowderSoftBlue  = Color(0xFFB7C7D6) // Bleu très doux

    // --- Alias fonctionnels ---
    val DarkBlue   = OxfordByNight
    val Blue       = NatoBlue
    val BlueMedium = BlueFedora
    val SplashBg   = PrairieDenim
    val TextPrimary = OxfordByNight
    val TextSecondary = BluePrince
    val ButtonBg   = NatoBlue
    val ButtonText = Foghorn

    // --- Fonds et Surfaces ---
    val Background = Color(0xFFF0F4F8) // Fond général (très doux)
    val SurfaceVariant = Color(0xFFE2EBF3) // Surface claire bleutée
    val CardBackground = Color(0xFFFAFCFF) // Fond de carte subtil
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Transparent = Color(0x00000000)
}

// Rétrocompatibilité avec les variables top-level existantes
val OxfordByNight   = YaGazPalette.OxfordByNight
val NatoBlue        = YaGazPalette.NatoBlue
val Nato2           = YaGazPalette.Nato2
val BlueFedora      = YaGazPalette.BlueFedora
val PrairieDenim    = YaGazPalette.PrairieDenim
val BluePrince      = YaGazPalette.BluePrince
val Foghorn         = YaGazPalette.Foghorn
val PowderSoftBlue  = YaGazPalette.PowderSoftBlue

val YaDarkBlue   = YaGazPalette.DarkBlue
val YaBlue       = YaGazPalette.Blue
val YaBlue2      = YaGazPalette.BlueMedium
val SplashBg     = YaGazPalette.SplashBg
val TextGrayBlue = YaGazPalette.TextSecondary
val ButtonBg     = YaGazPalette.ButtonBg
val ButtonText   = YaGazPalette.ButtonText

val AppBackground  = YaGazPalette.Background
val SurfaceVariant = YaGazPalette.SurfaceVariant
val CardBackground = YaGazPalette.CardBackground
