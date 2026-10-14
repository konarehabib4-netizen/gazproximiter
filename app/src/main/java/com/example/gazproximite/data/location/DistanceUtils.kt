package com.example.gazproximite.data.location

import android.location.Location
import java.util.Locale

fun calculerDistanceMetres(
    latitudeClient: Double,
    longitudeClient: Double,
    latitudeDepot: Double,
    longitudeDepot: Double
): Float {

    val resultat = FloatArray(1)

    Location.distanceBetween(
        latitudeClient,
        longitudeClient,
        latitudeDepot,
        longitudeDepot,
        resultat
    )

    return resultat[0]
}

// Convertit une distance en texte lisible.
// Exemples : 850f -> "850 m", 2340f -> "2,34 km".
// Float.NaN signifie que la position du client est inconnue.
fun formaterDistance(distanceMetres: Float): String {
    return if (distanceMetres.isNaN()) {
        "—"
    } else if (distanceMetres < 1000) {
        "${distanceMetres.toInt()} m"
    } else {
        String.format(Locale.FRANCE, "%.2f km", distanceMetres / 1000.0)
    }
}
