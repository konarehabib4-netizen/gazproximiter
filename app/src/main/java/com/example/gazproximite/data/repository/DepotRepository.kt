package com.example.gazproximite.data.repository

// Modèle représentant un dépôt
import com.example.gazproximite.data.model.Depot

// Modèles utilisés pour identifier la bouteille
import com.example.gazproximite.data.model.Format
import com.example.gazproximite.data.model.Marque

// Fonction qui construit une clé comme "petroci_b12"
import com.example.gazproximite.data.model.cleStock

// Permet d'utiliser Firestore
import com.google.firebase.firestore.FirebaseFirestore

// Permet de stocker latitude + longitude
import com.google.firebase.firestore.GeoPoint

// Permet d'utiliser la date/heure du serveur Firebase
import com.google.firebase.firestore.FieldValue

// Permet d'attendre la réponse de Firebase
import kotlinx.coroutines.tasks.await


// Repository responsable des opérations sur les dépôts.
class DepotRepository {

    // Récupération de l'instance de Firestore.
    private val db = FirebaseFirestore.getInstance()


    // =========================================================
    // 1. RÉCUPÉRER TOUS LES DÉPÔTS
    // =========================================================

    // Cette fonction récupère tous les dépôts
    // enregistrés dans la collection "depots".
    suspend fun recupererDepots(): List<Depot> {

        // On va dans la collection "depots",
        // on récupère tous les documents,
        // puis on attend la réponse de Firebase.
        val resultat = db
            .collection("depots")
            .get()
            .await()

        // Transformation des documents Firestore
        // en objets Kotlin de type Depot.
        return resultat.toObjects(Depot::class.java)
    }


    // =========================================================
    // 2. MODIFIER LE STOCK D'UNE BOUTEILLE
    // =========================================================

    // Cette fonction permet de modifier la disponibilité
    // d'une bouteille dans un dépôt.
    //
    // Exemple :
    //
    // modifierStock(
    //     "depot_angre_8",
    //     Marque.PETROCI,
    //     Format.B12,
    //     false
    // )
    //
    // signifie :
    //
    // Dans le dépôt "depot_angre_8",
    // la bouteille Petroci B12 n'est plus disponible.
    suspend fun modifierStock(
        depotId: String,
        marque: Marque,
        format: Format,
        disponible: Boolean
    ) {

        // On utilise notre fonction cleStock()
        // pour construire automatiquement la clé utilisée
        // dans Firestore.
        //
        // Exemple :
        //
        // Marque.PETROCI + Format.B12
        //             ↓
        //      "petroci_b12"
        val cle = cleStock(marque, format)


        // On va dans :
        //
        // Firestore
        //    ↓
        // collection "depots"
        //    ↓
        // document correspondant au dépôt
        val depotReference = db
            .collection("depots")
            .document(depotId)


        // On modifie uniquement la valeur correspondant
        // à cette bouteille dans la map "stock".
        //
        // Exemple :
        //
        // stock.petroci_b12 = false
        //
        // Les autres bouteilles ne sont pas modifiées.
        depotReference
            .update("stock.$cle", disponible)
            .await()
    }


    // =========================================================
    // 3. CRÉER UN DÉPÔT
    // =========================================================

    // Cette fonction permet de créer un nouveau dépôt.
    //
    // Le dépôt est associé à un vendeur grâce à son UID Firebase.
    //
    // Exemple :
    //
    // proprietaireUid = "Nx3b3moqJGZ8Di7nwQRKcO3V9PW2"
    //
    // Cela permet ensuite de savoir quel vendeur possède
    // quel dépôt.
    suspend fun creerDepot(
        depotId: String,
        nom: String,
        commune: String,
        telephone: String,
        latitude: Double,
        longitude: Double,
        proprietaireUid: String
    ) {

        // -----------------------------------------------------
        // Création du stock initial
        // -----------------------------------------------------
        //
        // Au départ, aucune bouteille n'est disponible.
        //
        // Nous allons créer automatiquement les 15 combinaisons :
        //
        // 5 marques × 3 formats = 15 bouteilles.
        //
        // Exemple :
        //
        // petroci_b6 = false
        // petroci_b12 = false
        // petroci_b28 = false
        //
        // etc.

        val stockInitial =
            mutableMapOf<String, Boolean>()

        // Quantités et prix de départ (le vendeur les renseigne ensuite).
        val quantitesInitiales =
            mutableMapOf<String, Long>()


        // Création des différentes combinaisons à partir
        // des enums Marque et Format, avec la même fonction
        // cleStock() que pour la lecture du stock.
        for (marque in Marque.entries) {

            for (format in Format.entries) {

                stockInitial[cleStock(marque, format)] = false
                quantitesInitiales[cleStock(marque, format)] = 0L
            }
        }


        // -----------------------------------------------------
        // Création du document du dépôt
        // -----------------------------------------------------

        val depot =
            hashMapOf(

                // Nom du dépôt.
                "nom" to nom,

                // Commune où se trouve le dépôt.
                "commune" to commune,

                // Numéro de téléphone du dépôt.
                "telephone" to telephone,

                // Position géographique du dépôt.
                "position" to GeoPoint(
                    latitude,
                    longitude
                ),

                // Lorsqu'un dépôt est créé,
                // il est fermé par défaut.
                "estOuvert" to false,

                // Heure d'ouverture par défaut.
                "heureOuverture" to "07:00",

                // Heure de fermeture par défaut.
                "heureFermeture" to "19:00",

                // UID du vendeur propriétaire du dépôt.
                "proprietaireUid" to proprietaireUid,

                // Stock initial du dépôt.
                "stock" to stockInitial,

                // Aucune bouteille au départ.
                "quantites" to quantitesInitiales,

                // Prix non renseignés au départ.
                "prix" to emptyMap<String, Long>(),

                // Firebase ajoute automatiquement
                // la date et l'heure du serveur.
                "misAJourLe" to FieldValue.serverTimestamp()
            )


        // -----------------------------------------------------
        // Enregistrement dans Firestore
        // -----------------------------------------------------

        db
            .collection("depots")
            .document(depotId)
            .set(depot)
            .await()
    }
}
