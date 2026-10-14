package com.example.gazproximite.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class LocationRepository(
    private val context: Context
) {
    // Service Android qui permet de récupérer la position du téléphone.
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Vérifie si la localisation (GPS) est activée dans les réglages du téléphone.
    // Même avec la permission, aucune position n'est disponible si elle est coupée.
    fun localisationActivee(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    // Vérifie si Android a déjà autorisé l'accès à la position.
    fun localisationAutorisee(): Boolean {
        val positionPrecise = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val positionApproximative = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // L'une des deux autorisations suffit pour continuer.
        return positionPrecise || positionApproximative
    }

    // Récupère la position sans bloquer l'interface.
    @SuppressLint("MissingPermission")
    suspend fun recupererPosition(): Location? {
        // Sécurité : ne tente pas de récupérer la position sans autorisation.
        if (!localisationAutorisee()) {
            return null
        }

        return try {
            // Demande d'abord la position actuelle avec une bonne précision.
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
            // Si aucune position actuelle n'est disponible,
            // utilise la dernière position connue.
                ?: fusedLocationClient.lastLocation.await()

        } catch (e: SecurityException) {
            // Évite un plantage si l'autorisation a été retirée.
            null
        } catch (e: Exception) {
            // Une erreur de localisation ne doit pas fermer l'application.
            null
        }
    }
}
