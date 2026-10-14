package com.example.gazproximite.ui.composants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonTopBar() {


    TopAppBar(
        navigationIcon = {
            IconButton(onClick = {
                println("menu")
            }) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
            }
            },
        title = {
            Text("GazProximité")
        }
    )


}
