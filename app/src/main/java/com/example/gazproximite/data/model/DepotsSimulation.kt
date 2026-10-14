package com.example.gazproximite.data.model

import com.google.firebase.firestore.GeoPoint

// Centre d'Abidjan, utilisé quand la position du client est inconnue.
val CENTRE_ABIDJAN = GeoPoint(5.3485, -4.0120)

// Construit un stock à partir de la liste des clés disponibles.
// Toutes les autres bouteilles sont marquées en rupture.
private fun stockSimule(vararg disponibles: Pair<Marque, Format>): Map<String, Boolean> {
    val enStock = disponibles.map { (marque, format) -> cleStock(marque, format) }.toSet()

    return Marque.entries.flatMap { marque ->
        Format.entries.map { format ->
            val cle = cleStock(marque, format)
            cle to (cle in enStock)
        }
    }.toMap()
}

// Dépôts FICTIFS affichés sur la carte en mode "Simulation".
// Ils servent à tester l'affichage sans remplir Firestore :
// ils ne sont jamais enregistrés dans la base.
val depotsSimulation: List<Depot> = listOf(
    Depot(
        id = "simulation_angre",
        nom = "Dépôt Angré 8e Tranche",
        commune = "Cocody",
        telephone = "+2250700000001",
        position = GeoPoint(5.3958, -3.9867),
        estOuvert = true,
        heureOuverture = "07:00",
        heureFermeture = "20:00",
        stock = stockSimule(
            Marque.PETROCI to Format.B6, Marque.PETROCI to Format.B12,
            Marque.TOTALENERGIES to Format.B12, Marque.SHELL to Format.B28
        ),
        estSimulation = true
    ),
    Depot(
        id = "simulation_riviera",
        nom = "Gaz Express Riviera 2",
        commune = "Cocody",
        telephone = "+2250700000002",
        position = GeoPoint(5.3667, -3.9686),
        estOuvert = true,
        heureOuverture = "06:30",
        heureFermeture = "21:00",
        stock = stockSimule(
            Marque.ORYX to Format.B6, Marque.ORYX to Format.B12,
            Marque.CORLAY to Format.B12, Marque.PETROCI to Format.B28
        ),
        estSimulation = true
    ),
    Depot(
        id = "simulation_yopougon",
        nom = "Dépôt Yopougon Siporex",
        commune = "Yopougon",
        telephone = "+2250700000003",
        position = GeoPoint(5.3423, -4.0805),
        estOuvert = false,
        heureOuverture = "07:00",
        heureFermeture = "19:00",
        stock = stockSimule(Marque.PETROCI to Format.B12),
        estSimulation = true
    ),
    Depot(
        id = "simulation_plateau",
        nom = "Plateau Gaz Service",
        commune = "Plateau",
        telephone = "+2250700000004",
        position = GeoPoint(5.3246, -4.0200),
        estOuvert = true,
        heureOuverture = "08:00",
        heureFermeture = "18:00",
        stock = stockSimule(
            Marque.TOTALENERGIES to Format.B6, Marque.TOTALENERGIES to Format.B12,
            Marque.SHELL to Format.B6, Marque.SHELL to Format.B12
        ),
        estSimulation = true
    ),
    Depot(
        id = "simulation_marcory",
        nom = "Dépôt Marcory Zone 4",
        commune = "Marcory",
        telephone = "+2250700000005",
        position = GeoPoint(5.2965, -3.9790),
        estOuvert = true,
        heureOuverture = "07:00",
        heureFermeture = "20:30",
        stock = stockSimule(
            Marque.PETROCI to Format.B6, Marque.PETROCI to Format.B12,
            Marque.PETROCI to Format.B28, Marque.CORLAY to Format.B6
        ),
        estSimulation = true
    ),
    Depot(
        id = "simulation_abobo",
        nom = "Abobo Gare Gaz",
        commune = "Abobo",
        telephone = "+2250700000006",
        position = GeoPoint(5.4210, -4.0170),
        estOuvert = false,
        heureOuverture = "07:30",
        heureFermeture = "19:00",
        stock = stockSimule(Marque.ORYX to Format.B12, Marque.SHELL to Format.B12),
        estSimulation = true
    ),
    Depot(
        id = "simulation_treichville",
        nom = "Treichville Bouteilles",
        commune = "Treichville",
        telephone = "+2250700000007",
        position = GeoPoint(5.2930, -4.0080),
        estOuvert = true,
        heureOuverture = "06:00",
        heureFermeture = "22:00",
        stock = stockSimule(
            Marque.TOTALENERGIES to Format.B12, Marque.ORYX to Format.B28,
            Marque.CORLAY to Format.B12
        ),
        estSimulation = true
    ),
    Depot(
        id = "simulation_koumassi",
        nom = "Koumassi Remblais Gaz",
        commune = "Koumassi",
        telephone = "+2250700000008",
        position = GeoPoint(5.2985, -3.9480),
        estOuvert = true,
        heureOuverture = "07:00",
        heureFermeture = "19:30",
        stock = stockSimule(Marque.PETROCI to Format.B12, Marque.SHELL to Format.B6),
        estSimulation = true
    )
)
