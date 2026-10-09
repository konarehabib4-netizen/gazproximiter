// Ce fichier est dans le même package que Marque et Format,
// donc on peut les utiliser directement, sans import.
package com.example.gazproximite.data.model

/**
 * Construit la clé utilisée dans la map "stock" d'un dépôt Firestore.
 *
 * Dans Firestore, le stock est stocké sous la forme :
 *   stock = { "petroci_b12": true, "shell_b6": false, ... }
 * Cette fonction fabrique donc le texte "marque_format" en minuscules.
 *
 * Exemples :
 *   cleStock(Marque.PETROCI, Format.B12)       -> "petroci_b12"
 *   cleStock(Marque.TOTALENERGIES, Format.B6)  -> "totalenergies_b6"
 *
 * @param marque la marque de la bouteille (ex. Marque.SHELL)
 * @param format le format de la bouteille (ex. Format.B28)
 * @return la clé correspondante, telle qu'écrite dans Firestore
 */
fun cleStock(marque: Marque, format: Format): String {
    // "name" est une propriété automatique de tout enum :
    // elle contient le nom de la valeur, écrit comme dans le code.
    // Ex. : Marque.PETROCI.name vaut "PETROCI"
    val nomMarque = marque.name

    // Même chose pour le format. Ex. : Format.B12.name vaut "B12"
    val nomFormat = format.name

    // On assemble les deux textes avec un "_" au milieu.
    // "$nomMarque" insère la valeur de la variable dans le texte.
    // Ensuite, lowercase() passe tout en minuscules,
    // car dans Firestore les clés sont écrites en minuscules.
    return "${nomMarque}_${nomFormat}".lowercase()
}