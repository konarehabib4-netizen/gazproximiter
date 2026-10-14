package com.example.gazproximite.ui.composants

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.gazproximite.data.model.Depot

// Ouvre le composeur téléphonique avec le numéro du dépôt.
fun appelerDepot(context: Context, telephone: String) {
    val intent = Intent(
        Intent.ACTION_DIAL,
        Uri.fromParts("tel", telephone.trim(), null)
    )
    context.startActivity(intent)
}

// Ouvre Google Maps (ou une autre appli de cartes) sur la position du dépôt.
fun ouvrirItineraire(context: Context, depot: Depot) {
    val position = depot.position ?: return

    val coordonnees = "${position.latitude},${position.longitude}"
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:$coordonnees?q=$coordonnees(${Uri.encode(depot.nom)})")
    )

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Aucune application de cartes n'est installée.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
