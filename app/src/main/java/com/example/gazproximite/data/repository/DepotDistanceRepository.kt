package com.example.gazproximite.data.repository
import android.content.Context
import com.example.gazproximite.data.location.LocationRepository
import com.example.gazproximite.data.location.calculerDistanceMetres
import com.example.gazproximite.data.model.DepotAvecDistance
import com.example.gazproximite.data.model.depotsSimulation

class DepotDistanceRepository(context: Context) {

    // Permet de lire les dépôts enregistrés dans Firestore.
    private val depotRepository = DepotRepository()

    // Permet de récupérer la position du client.
    private val locationRepository = LocationRepository(context)

    // Retourne les dépôts avec leur distance par rapport au client,
    // ou null si la position du client n'a pas pu être obtenue.
    // Avec inclureSimulation, les dépôts fictifs de démonstration sont ajoutés.
    suspend fun recupererDepotsAvecDistance(
        inclureSimulation: Boolean = false
    ): List<DepotAvecDistance>? {

        // Étape 1 : récupérer la position du client.
        val positionClient = locationRepository.recupererPosition()
            ?: return null

        // Étape 2 : récupérer les dépôts enregistrés dans Firestore.
        val depots = depotRepository.recupererDepots() +
                if (inclureSimulation) depotsSimulation else emptyList()

        // Étape 3 : calculer la distance pour chaque dépôt.
        return depots.mapNotNull { depot ->

            // Un dépôt sans coordonnées ne peut pas être localisé.
            val positionDepot = depot.position

            if (positionDepot == null) {
                null
            } else {
                val distance = calculerDistanceMetres(
                    positionClient.latitude,
                    positionClient.longitude,
                    positionDepot.latitude,
                    positionDepot.longitude
                )

                // On associe le dépôt à sa distance en mètres.
                DepotAvecDistance(
                    depot = depot,
                    distanceMetres = distance
                )
            }
        }
            // Étape 4 : afficher les dépôts du plus proche au plus éloigné.
            .sortedBy { it.distanceMetres }
    }
}
