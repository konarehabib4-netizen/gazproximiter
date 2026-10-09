
package com.example.gazproximite.data.location

import android.location.Location

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

