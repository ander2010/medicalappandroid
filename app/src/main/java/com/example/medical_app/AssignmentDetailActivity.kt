package com.example.medical_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.tuapp.network.ApiClient
import com.tuapp.network.ApiService
import com.tuapp.network.PedidoRequest
import com.tuapp.network.UpdatePedidoPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

// ——————————— Models ———————————

data class DrugItem(
    val id: Int,
    val code: String,
    val nombre: String,
    val nombremarca: String,
    val dosis: String,
    val tamano: String,
    val price: Double
)

data class DrugSection(
    val category: String,
    val items: List<DrugItem>
)

// ——————————— Activity ———————————

class AssignmentDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AssignmentDetailScreen(onBack = { finish() }) }
    }
}

// ——————————— Composable ———————————

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailScreen(onBack: () -> Unit) {
    val context           = LocalContext.current
    val api               = ApiClient.instance.create(ApiService::class.java)
    val scope             = rememberCoroutineScope()
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    // Config
    val userId       = 1
    val asignacionId = 12

    // — Estados
    var sections         by remember { mutableStateOf(listOf<DrugSection>()) }
    val selected         = remember { mutableStateMapOf<Int, Boolean>() }
    var budget           by remember { mutableStateOf(0.0) }
    var remainingBudget  by remember { mutableStateOf(0.0) }
    var insuranceName    by remember { mutableStateOf("") }
    val userName         = "Jorge"

    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var pedidoId         by remember { mutableStateOf<Int?>(null) }
    var pedidoEnProgreso by remember { mutableStateOf(false) }
    var pedidoFinalizado by remember { mutableStateOf(false) }
    var isLoading        by remember { mutableStateOf(true) }

    // Si las meds llegan antes que sections, las aplicamos luego
    var pendingMeds      by remember { mutableStateOf<List<Int>?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // ——— Helpers de UI/negocio ———

    fun calcularTotal(): Double =
        selected.filter { it.value }.keys.sumOf { id ->
            sections.flatMap { it.items }.first { it.id == id }.price
        }

    fun totalRedondeado(): Double {
        val t = calcularTotal()
        return if (t.isFinite()) String.format("%.2f", t).toDouble() else 0.0
    }

    fun onToggle(id: Int, price: Double) {
        val willSelect = selected[id] != true
        val currentTotal = calcularTotal()
        val newTotal = if (willSelect) currentTotal + price else currentTotal - price
        if (newTotal <= budget) {
            selected[id] = willSelect
            remainingBudget = budget - newTotal
        } else {
            Toast.makeText(context, "❌ Límite de presupuesto alcanzado", Toast.LENGTH_SHORT).show()
        }
    }

    fun aplicarSeleccionPrevia(meds: List<Int>) {
        if (sections.isEmpty()) {
            // Aún no hay catálogo, guárdalas para aplicarlas cuando lleguen
            pendingMeds = meds
            return
        }
        selected.clear()
        val allItems = sections.flatMap { it.items }
        var total = 0.0
        meds.forEach { id ->
            val item = allItems.firstOrNull { it.id == id }
            if (item != null) {
                selected[id] = true
                total += item.price
            }
        }
        remainingBudget = (budget - total).coerceAtLeast(0.0)
        pendingMeds = null
    }

    suspend fun mostrarErrorBackend(
        resp: retrofit2.Response<*>,
        onUi: suspend (String) -> Unit
    ) {
        val code = resp.code()
        val msg = try { resp.errorBody()?.string()?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
        onUi("Error ($code)${if (!msg.isNullOrBlank()) ": $msg" else ""}")
    }

    fun parsePedidoBody(body: Map<String, Any>): Triple<Int?, String?, List<Int>> {
        val id = (body["id"] as? Number)?.toInt() ?: (body["pk"] as? Number)?.toInt()
        val status = (body["status"] as? String) ?: (body["estado"] as? String)
        val medsRaw = body["medicinas"] ?: body["meds"] ?: body["items"]
        val medsIds: List<Int> = when (medsRaw) {
            is List<*> -> medsRaw.mapNotNull { el ->
                when (el) {
                    is Number -> el.toInt()
                    is String -> el.toIntOrNull()
                    is Map<*, *> -> ((el["id"] as? Number)?.toInt() ?: (el["pk"] as? Number)?.toInt())
                    else -> null
                }
            }
            is String -> medsRaw.split(",").mapNotNull { it.trim().toIntOrNull() }
            else -> emptyList()
        }
        return Triple(id, status, medsIds)
    }

    suspend fun fetchPedidoEnProgreso(api: ApiService, userId: Int): Triple<Int?, String?, List<Int>>? {
        runCatching { api.getPedidoEnProgreso(userId) }.getOrNull()?.let { resp ->
            if (resp.isSuccessful && resp.body() != null && resp.code() !in listOf(204, 404)) {
                return parsePedidoBody(resp.body()!!)
            }
        }
        runCatching { api.getPedidoEnProgresoAlt(userId) }.getOrNull()?.let { resp ->
            if (resp.isSuccessful && resp.body() != null && resp.code() !in listOf(204, 404)) {
                return parsePedidoBody(resp.body()!!)
            }
        }
        return null
    }

    // ——— Carga inicial (catálogo + primer fetch de pedido) ———
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val aResp: Response<Map<String, Any>> = withContext(Dispatchers.IO) {
                api.getAsignacionMensual(asignacionId).execute()
            }
            if (aResp.isSuccessful) {
                aResp.body()?.let { body ->
                    val plan = body["plan_salud"] as Map<*, *>
                    budget = (plan["presupuesto_mensual"] as Number).toDouble()
                    remainingBudget = budget
                    insuranceName = plan["seguro_medico"].toString()

                    val drugs = body["drugs"] as List<Map<String, Any>>
                    sections = drugs
                        .groupBy { (it["categoria"] as? Map<*, *>)?.get("nombre") ?: "Uncategorized" }
                        .map { (cat, list) ->
                            DrugSection(
                                category = cat.toString(),
                                items = list.map {
                                    DrugItem(
                                        id          = (it["id"] as Number).toInt(),
                                        code        = it["code"].toString(),
                                        nombre      = it["nombre"].toString(),
                                        nombremarca = it["nombremarca"].toString(),
                                        dosis       = it["dosis"].toString(),
                                        tamano      = it["tamano"].toString(),
                                        price       = (it["costototal"] as Number).toDouble()
                                    )
                                }
                            )
                        }

                    // 1ª lectura del pedido existente
                    val triple = fetchPedidoEnProgreso(api, userId)
                    if (triple != null) {
                        val (pid, status, meds) = triple
                        pedidoId = pid
                        pedidoEnProgreso = status == "P"
                        pedidoFinalizado = status == "C"
                        if (meds.isNotEmpty()) aplicarSeleccionPrevia(meds)
                    } else {
                        pedidoId = null
                        pedidoEnProgreso = false
                        pedidoFinalizado = false
                    }
                }
            }
        } finally {
            isLoading = false
        }
    }

    // ——— Si sections llega y había meds pendientes, aplícalas ———
    LaunchedEffect(sections) {
        pendingMeds?.let { meds ->
            if (sections.isNotEmpty()) aplicarSeleccionPrevia(meds)
        }
    }

    // ——— Reload al volver (ON_RESUME): solo re-lee el pedido y aplica selección ———
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    // Evita parpadeo: no toques sections/budget; solo pedido/selección
                    val triple = fetchPedidoEnProgreso(api, userId)
                    triple?.let { (pid, status, meds) ->
                        pedidoId = pid
                        pedidoEnProgreso = status == "P"
                        pedidoFinalizado = status == "C"
                        if (meds.isNotEmpty()) aplicarSeleccionPrevia(meds)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ——— Acciones ———

    fun handleSave() {
        scope.launch {
            isLoading = true

            // Evita duplicados
            val existente = fetchPedidoEnProgreso(api, userId)
            if (existente?.first != null) {
                val (pid, status, meds) = existente
                pedidoId = pid
                pedidoEnProgreso = status == "P"
                pedidoFinalizado = status == "C"
                if (meds.isNotEmpty()) aplicarSeleccionPrevia(meds)
                isLoading = false
                return@launch
            }

            val meds = selected.filter { it.value }.keys.map { it.toInt() }
            val total = totalRedondeado()
            val payload = PedidoRequest(user = userId, medicinas = meds, costototal = total)

            withContext(Dispatchers.IO) {
                runCatching { api.crearPedido(payload) }.onSuccess { r ->
                    if (r.isSuccessful) {
                        val again = fetchPedidoEnProgreso(api, userId)
                        withContext(Dispatchers.Main) {
                            if (again != null) {
                                val (pid2, status2, meds2) = again
                                pedidoId = pid2
                                pedidoEnProgreso = status2 == "P"
                                pedidoFinalizado = status2 == "C"
                                if (meds2.isNotEmpty()) aplicarSeleccionPrevia(meds2)
                            } else {
                                pedidoEnProgreso = true
                            }
                            scope.launch { snackbarHostState.showSnackbar("✅ Se ha creado el pedido") }
                            Toast.makeText(context, "Se ha creado el pedido", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            scope.launch {
                                mostrarErrorBackend(r) { msg ->
                                    snackbarHostState.showSnackbar(msg)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        val msg = "Error de red al guardar"
                        snackbarHostState.showSnackbar(msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            isLoading = false
        }
    }

    fun handleUpdate() {
        val id = pedidoId ?: return

        val meds: List<Int> = selected.filter { it.value }.keys.map { it.toInt() }
        val total = totalRedondeado()

        val payload = UpdatePedidoPayload(
            medicinas = meds,
            costototal = total,
            status = "P"
        )

        scope.launch(Dispatchers.IO) {
            runCatching { api.updatePedido(id, payload) }.onSuccess { r ->
                if (r.isSuccessful) {
                    val triple = fetchPedidoEnProgreso(api, userId)
                    withContext(Dispatchers.Main) {
                        triple?.let { (pid, status, medsIds) ->
                            pedidoId = pid
                            pedidoEnProgreso = status == "P"
                            pedidoFinalizado = status == "C"
                            if (medsIds.isNotEmpty()) aplicarSeleccionPrevia(medsIds)
                        }
                        scope.launch { snackbarHostState.showSnackbar("✅ Se ha actualizado el pedido") }
                        Toast.makeText(context, "Se ha actualizado el pedido", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        scope.launch {
                            mostrarErrorBackend(r) { msg ->
                                snackbarHostState.showSnackbar(msg)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    val msg = "Error de red al actualizar"
                    snackbarHostState.showSnackbar(msg)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun handleFinalizar() {
        val id = pedidoId ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { api.finalizarPedido(id) }.onSuccess { r ->
                if (r.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        pedidoFinalizado = true
                        pedidoEnProgreso = false
                        scope.launch { snackbarHostState.showSnackbar("✅ Pedido finalizado") }
                        Toast.makeText(context, "Pedido finalizado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val msg = "Error al finalizar (${r.code()})"
                        snackbarHostState.showSnackbar(msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    val msg = "Error de red al finalizar"
                    snackbarHostState.showSnackbar(msg)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ——— UI ———
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Assignments", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF2979FF))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "$userName, tienes $$budget y te quedan $${"%.2f".format(remainingBudget)} — $insuranceName",
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                } else {
                    when {
                        pedidoFinalizado -> {
                            Text("✅ Tu pedido ha sido completado", color = Color(0xFF4CAF50))
                        }
                        (pedidoId != null) || pedidoEnProgreso -> {
                            Button(
                                onClick = { handleUpdate() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
                            ) { Text("Update", color = Color.White) }
                            Button(
                                onClick = { handleFinalizar() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) { Text("Finalizar Pedido", color = Color.White) }
                        }
                        else -> {
                            Button(
                                onClick = { handleSave() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
                            ) { Text("Save", color = Color.White) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                sections.forEach { section ->
                    item {
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCategory =
                                                if (expandedCategory == section.category) null else section.category
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(section.category, fontWeight = FontWeight.Bold)
                                    Icon(
                                        imageVector = if (expandedCategory == section.category)
                                            Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }

                                if (expandedCategory == section.category) {
                                    section.items.forEach { item ->
                                        Divider()
                                        ListItem(
                                            leadingContent = {
                                                Checkbox(
                                                    checked = selected[item.id] == true,
                                                    onCheckedChange = { onToggle(item.id, item.price) }
                                                )
                                            },
                                            headlineContent = { Text("${item.code} | ${item.nombre}") },
                                            supportingContent = {
                                                Text("${item.nombremarca} • ${item.dosis} • ${item.tamano}")
                                            },
                                            trailingContent = { Text("$${"%.2f".format(item.price)}") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
