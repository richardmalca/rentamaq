package pe.rentamaq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import pe.rentamaq.core.data.CatalogRepository
import pe.rentamaq.core.data.local.RentamaqDatabase
import pe.rentamaq.core.ui.RentamaqApp
import pe.rentamaq.feature.explore.CatalogViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK))
        setContent {
            val model = viewModel {
                val database = Room.databaseBuilder(applicationContext, RentamaqDatabase::class.java, "rentamaq.db").build()
                CatalogViewModel(CatalogRepository(database.catalog()))
            }
            RentamaqApp(model)
        }
    }
}
