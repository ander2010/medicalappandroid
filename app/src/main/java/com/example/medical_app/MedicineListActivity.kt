package com.example.medical_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medical_app.ui.components.AppScreen
import com.tuapp.network.ApiClient
import com.tuapp.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

data class Medicine(
    val nombre: String,
    val description: String?,
    val dosis: String?,
    val tamano: String?,
    val costo: Double?
)

class MedicineListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val category = intent.getStringExtra("category") ?: ""
        setContent {
            MedicineListScreen(category = category, onBack = { finish() })
        }
    }
}

@Composable
fun MedicineListScreen(category: String, onBack: () -> Unit) {
    val api = ApiClient.instance.create(ApiService::class.java)
    val scope = rememberCoroutineScope()

    var medicines by remember { mutableStateOf(listOf<Medicine>()) }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    // ✅ MISMA LÓGICA: Llamada API al cargar
    LaunchedEffect(category) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = api.getMedicinasByCategoria(category).awaitResponse()
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    medicines = body.map {
                        Medicine(
                            nombre = it["nombre"]?.toString() ?: "",
                            description = it["description"]?.toString(),
                            dosis = it["dosis"]?.toString(),
                            tamano = it["tamano"]?.toString(),
                            costo = it["costo"]?.toString()?.toDoubleOrNull()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ✅ MISMO HEADER VIOLETA (AppScreen)
    AppScreen(
        title = category.ifBlank { "Medicines" },
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(medicines) { index, med ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedIndex = if (expandedIndex == index) null else index
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = med.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (expandedIndex == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (expandedIndex == index) {
                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = med.description ?: "Sin descripción",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            InfoRow("Dosis", med.dosis ?: "No disponible")
                            InfoRow("Tamaño", med.tamano ?: "No disponible")
                            InfoRow("Costo", med.costo?.let { "$$it" } ?: "0")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
