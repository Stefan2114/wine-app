package com.example.wine_app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wine_app.ui.add_edit_wine.AddEditWineScreen
import com.example.wine_app.ui.wine_list.WineListScreen
import com.example.wine_app.util.AddEditWine
import com.example.wine_app.util.WineList
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = WineList
            ) {
                composable<WineList> {
                    WineListScreen(onNavigate = {
                        navController.navigate(it.destination)
                    })
                }
                composable<AddEditWine> {
                    AddEditWineScreen(onPopBackStack = {
                        navController.popBackStack()
                    })
                }
            }
        }
    }
}