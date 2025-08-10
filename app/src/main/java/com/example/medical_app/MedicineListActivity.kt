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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(category: String, onBack: () -> Unit) {
    val api = ApiClient.instance.create(ApiService::class.java)
    val scope = rememberCoroutineScope()

    var medicines by remember { mutableStateOf(listOf<Medicine>()) }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    // Llamada API al cargar
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(category, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF2979FF)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            itemsIndexed(medicines) { index, med ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Icon(
                                imageVector = if (expandedIndex == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand"
                            )
                        }

                        if (expandedIndex == index) {
                            Text(
                                text = med.description ?: "Sin descripción",
                                fontSize = 14.sp,
                                color = Color(0xFF444444),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )

                            InfoRow("Dosis:", med.dosis ?: "No disponible")
                            InfoRow("Tamaño:", med.tamano ?: "No disponible")
                            InfoRow("Costo:", med.costo?.let { "$$it" } ?: "0")
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
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Text("$label ", fontWeight = FontWeight.Bold)
        Text(value)
    }
}
