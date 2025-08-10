package com.example.medical_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.*

data class Pedido(
    val id: Int,
    val fecha_pedido: String,
    val costototal: Double,
    val status: String,
    val user_name: String?,
    val medicinas: List<Medicina>
)

data class Medicina(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val categoria: String
)

class AssignmentHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssignmentHistoryScreen(onBack = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentHistoryScreen(onBack: () -> Unit) {
    val api = ApiClient.instance.create(ApiService::class.java)
    val scope = rememberCoroutineScope()

    var pedidos by remember { mutableStateOf(listOf<Pedido>()) }
    var expandedId by remember { mutableStateOf<Int?>(null) }

    // Cargar historial
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = api.getHistorial(1).awaitResponse()
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    pedidos = body.map {
                        Pedido(
                            id = (it["id"] as? Double)?.toInt() ?: 0,
                            fecha_pedido = it["fecha_pedido"]?.toString() ?: "",
                            costototal = (it["costototal"] as? Double) ?: 0.0,
                            status = it["status"]?.toString() ?: "",
                            user_name = it["user_name"]?.toString(),
                            medicinas = (it["medicinas"] as? List<Map<String, Any>>)?.map { med ->
                                Medicina(
                                    id = (med["id"] as? Double)?.toInt() ?: 0,
                                    nombre = med["nombre"]?.toString() ?: "",
                                    descripcion = med["descripcion"]?.toString() ?: "",
                                    categoria = med["categoria"]?.toString() ?: ""
                                )
                            } ?: emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Encabezado
        TopAppBar(
            title = { Text("Assignment History", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFF2979FF))
        )

        if (pedidos.isNotEmpty()) {
            Text(
                text = "Hola ${pedidos.first().user_name ?: "Usuario"}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(pedidos) { pedido ->
                PedidoCard(
                    pedido = pedido,
                    expanded = expandedId == pedido.id,
                    onExpandToggle = {
                        expandedId = if (expandedId == pedido.id) null else pedido.id
                    }
                )
            }
        }
    }
}

@Composable
fun PedidoCard(pedido: Pedido, expanded: Boolean, onExpandToggle: () -> Unit) {
    val date = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        outputFormat.format(inputFormat.parse(pedido.fecha_pedido) ?: Date())
    } catch (e: Exception) {
        pedido.fecha_pedido
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Fecha: $date", fontSize = 16.sp)
            Text("Costo total: $${pedido.costototal}", fontSize = 16.sp)
            Text(
                "Estado: ${if (pedido.status == "C") "Completado" else "En progreso"}",
                fontSize = 16.sp
            )

            Button(
                onClick = onExpandToggle,
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
            ) {
                Text(
                    if (expanded) "Ocultar Medicinas" else "Mostrar Medicinas",
                    color = Color.White
                )
            }

            if (expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    pedido.medicinas.forEach { med ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text("🩺 ${med.nombre}", fontWeight = FontWeight.Bold)
                            Text("📝 ${med.descripcion}", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            Text("🏷 Categoría ID: ${med.categoria}")
                        }
                    }
                }
            }
        }
    }
}
