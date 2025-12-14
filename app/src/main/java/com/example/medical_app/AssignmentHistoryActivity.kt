package com.example.medical_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        setContent { AssignmentHistoryScreen(onBack = { finish() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentHistoryScreen(onBack: () -> Unit) {
    val api = ApiClient.instance.create(ApiService::class.java)
    val scope = rememberCoroutineScope()

    var pedidos by remember { mutableStateOf(listOf<Pedido>()) }
    var expandedId by remember { mutableStateOf<Int?>(null) }

    // ✅ SOLO UI: usar violeta del theme
    val violet = MaterialTheme.colorScheme.primary

    // Cargar historial (MISMA LOGICA)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Assignment History",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = violet,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {

            if (pedidos.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Hola ${pedidos.first().user_name ?: "Usuario"}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Aquí está el historial de tus pedidos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            } else {
                // Empty state simple (solo UI)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No hay historial todavía",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Cuando hagas tu primer pedido, aparecerá aquí.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pedidos) { pedido ->
                    PedidoCardProUI(
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
}

@Composable
fun PedidoCardProUI(pedido: Pedido, expanded: Boolean, onExpandToggle: () -> Unit) {
    val violet = MaterialTheme.colorScheme.primary

    val date = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        outputFormat.format(inputFormat.parse(pedido.fecha_pedido) ?: Date())
    } catch (e: Exception) {
        pedido.fecha_pedido
    }

    val isCompleted = pedido.status == "C"
    val statusLabel = if (isCompleted) "Completado" else "En progreso"

    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Fecha",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AssistChip(
                    onClick = { },
                    label = { Text(statusLabel) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFF3E5F5),
                        labelColor = if (isCompleted) Color(0xFF2E7D32) else Color(0xFF6A1B9A)
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$${"%.2f".format(pedido.costototal)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(10.dp))

            // “Mostrar/Ocultar” como fila clickeable pro (en vez de botón azul)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Ocultar medicinas" else "Mostrar medicinas",
                    color = violet,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = violet
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pedido.medicinas.forEach { med ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = med.nombre,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = med.descripcion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Categoría: ${med.categoria}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
