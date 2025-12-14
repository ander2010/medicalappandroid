package com.example.medical_app

import android.os.Bundle
import android.util.Log
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
import com.tuapp.network.UserInformationResponse
import androidx.compose.foundation.shape.RoundedCornerShape

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





    val sessionManager = remember { SessionManager(context) }

    var userData by remember { mutableStateOf(UserData()) }
    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

// ✅ Traer el valor real guardado
    val savedEmail = remember { sessionManager.getEmail() }  // email guardado en login
    val savedToken = remember { sessionManager.getToken() }



    // ⚠️ Debe ser el PK de `Usuario` (modelo que usa Pedido.user)
    val userId       =  remember { sessionManager.getUserId() }
    var userName by remember { mutableStateOf(savedEmail ?: "") }
    var mesCompletado by remember { mutableStateOf(false) }
    var msgMesCompletado by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        Log.d("HOME_SESSION", "=========== SESSION READ ===========")
        Log.d("HOME_SESSION", "savedEmail: $savedEmail")
        Log.d("HOME_SESSION", "User_id: $userId")
        Log.d("HOME_SESSION", "savedToken: ${savedToken?.take(20)}...")
        Log.d("HOME_SESSION", "isLoggedIn: ${sessionManager.isLoggedIn()}")
        Log.d("HOME_SESSION", "===================================")
    }
    // — Estados
    var sections         by remember { mutableStateOf(listOf<DrugSection>()) }
    val selected         = remember { mutableStateMapOf<Int, Boolean>() }
    var budget           by remember { mutableStateOf(0.0) }
    var remainingBudget  by remember { mutableStateOf(0.0) }
    // —— ASIGNACIONES POR PLAN ——
    var asignacionesPorPlan by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedAsignacionId by remember { mutableStateOf<Int?>(null) }

    var insuranceName    by remember { mutableStateOf("") }
    var userInfo by remember { mutableStateOf<UserInformationResponse?>(null) }
    var asignacionMensualTemp by remember { mutableStateOf<Map<String, Any>?>(null) }


    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var pedidoId         by remember { mutableStateOf<Int?>(null) }
    var pedidoEnProgreso by remember { mutableStateOf(false) }
    var pedidoFinalizado by remember { mutableStateOf(false) }
    var isLoading        by remember { mutableStateOf(true) }

    // Si las meds llegan antes que sections, las aplicamos luego
    var pendingMeds      by remember { mutableStateOf<List<Int>?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(savedEmail) {

        Log.d("USER_INFO", "================ USER INFO REQUEST ================")
        Log.d("USER_INFO", "Identifier (email): $savedEmail")

        if (savedEmail.isNullOrBlank()) {
            Log.e("USER_INFO", "❌ No email found in session")
            errorMsg = "No email found in session"
            return@LaunchedEffect
        }

        loading = true
        errorMsg = ""

        try {
            val resp = withContext(Dispatchers.IO) {
                api.getUserInformation(savedEmail).execute()
            }

            Log.d("USER_INFO", "HTTP code: ${resp.code()}")
            Log.d("USER_INFO", "isSuccessful: ${resp.isSuccessful}")

            if (resp.isSuccessful && resp.body() != null) {

                val info = resp.body()!!
                userInfo = info

                // —— LOGS DEL USUARIO ——
                Log.d("USER_INFO", "---------- USER ----------")
                Log.d("USER_INFO", "user_id: ${info.user_id}")
                Log.d("USER_INFO", "user_name: ${info.user_name}")
                Log.d("USER_INFO", "shipping_address: ${info.shipping_address}")
                Log.d("USER_INFO", "user_monthly_budget: ${info.user_monthly_budget}")

                // —— LOGS DEL PLAN ——
                val plan = info.health_plan
                Log.d("USER_INFO", "---------- HEALTH PLAN ----------")
                Log.d("USER_INFO", "plan_id: ${plan?.id}")
                Log.d("USER_INFO", "plan_name: ${plan?.name}")
                Log.d("USER_INFO", "plan_description: ${plan?.description}")
                Log.d("USER_INFO", "plan_monthly_budget: ${plan?.monthly_budget}")

                // —— LOGS DEL SEGURO ——
                val insurance = plan?.health_insurance
                Log.d("USER_INFO", "---------- INSURANCE ----------")
                Log.d("USER_INFO", "insurance_name: ${insurance?.name}")
                Log.d("USER_INFO", "insurance_description: ${insurance?.description}")
                Log.d("USER_INFO", "insurance_email: ${insurance?.email}")
                Log.d("USER_INFO", "insurance_address: ${insurance?.address}")
                Log.d("USER_INFO", "insurance_phone: ${insurance?.phone}")
                Log.d("USER_INFO", "insurance_id_number: ${insurance?.insurance_id_number}")

                Log.d("USER_INFO", "==============================================")

                // —— Actualizar UI (mínimo)
                insuranceName = insurance?.name ?: insuranceName
                budget = plan?.monthly_budget ?: budget
                remainingBudget = budget

                // Si quieres el nombre real en pantalla:
                // userName = info.user_name

            } else {
                val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                Log.e("USER_INFO", "❌ Backend error: ${resp.code()} - $err")
                errorMsg = "Error (${resp.code()})"
            }

        } catch (e: Exception) {
            Log.e("USER_INFO", "❌ Exception calling getUserInformation", e)
            errorMsg = "Exception: ${e.message}"
        } finally {
            loading = false
            Log.d("USER_INFO", "================ END USER INFO =================")
        }
    }

    LaunchedEffect(userInfo,mesCompletado) {

        if (mesCompletado) {
            Log.d("PLAN_ASIGNACIONES", "mesCompletado=true -> NO fetch asignaciones")
            return@LaunchedEffect
        }

        val planId = userInfo?.health_plan?.id ?: return@LaunchedEffect
//        val planId = userInfo?.health_plan?.id

        Log.d("PLAN_ASIGNACIONES", "================ PLAN ASIGNACIONES REQUEST ================")
        Log.d("PLAN_ASIGNACIONES", "userInfo present: ${userInfo != null}")
        Log.d("PLAN_ASIGNACIONES", "planId: $planId")

        if (planId == null) {
            Log.e("PLAN_ASIGNACIONES", "❌ planId is NULL, cannot fetch asignaciones")
            return@LaunchedEffect
        }

        try {
            val resp = withContext(Dispatchers.IO) {
                api.getAsignacionesPorPlan(planId)
            }

            Log.d("PLAN_ASIGNACIONES", "HTTP code: ${resp.code()}")
            Log.d("PLAN_ASIGNACIONES", "isSuccessful: ${resp.isSuccessful}")

            if (resp.isSuccessful && resp.body() != null) {

                val body = resp.body()!!
                asignacionesPorPlan = body

                Log.d("PLAN_ASIGNACIONES", "✔ asignaciones count: ${body.size}")

                body.forEachIndexed { index, item ->
                    Log.d("PLAN_ASIGNACIONES", "---- ASIGNACION [$index] ----")
                    item.forEach { (k, v) ->
                        Log.d("PLAN_ASIGNACIONES", "$k = $v")
                    }
                }

                // 👉 Seleccionamos la primera asignación automáticamente
                val firstId = (body.firstOrNull()?.get("id") as? Number)?.toInt()

                selectedAsignacionId = firstId

                Log.d("PLAN_ASIGNACIONES", "selectedAsignacionId: $selectedAsignacionId")

            } else {
                val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                Log.e(
                    "PLAN_ASIGNACIONES",
                    "❌ Backend error (${resp.code()}): ${err ?: "no error body"}"
                )
            }

        } catch (e: Exception) {
            Log.e("PLAN_ASIGNACIONES", "❌ Exception fetching asignaciones", e)
        }

        Log.d("PLAN_ASIGNACIONES", "================ END PLAN ASIGNACIONES =================")
    }

    LaunchedEffect(selectedAsignacionId,mesCompletado) {

        Log.d("ASIGNACION_DETALLE", "================ ASIGNACION MENSUAL REQUEST ================")
        Log.d("ASIGNACION_DETALLE", "selectedAsignacionId: $selectedAsignacionId")
        if (mesCompletado) {
            Log.d("ASIGNACION_DETALLE", "mesCompletado=true -> NO fetch asignacion/drugs")
            return@LaunchedEffect
        }

        val aId = selectedAsignacionId ?: return@LaunchedEffect
//        val aId = selectedAsignacionId
        if (aId == null) {
            Log.e("ASIGNACION_DETALLE", "❌ selectedAsignacionId is NULL. Skipping getAsignacionMensual().")
            return@LaunchedEffect
        }

        try {
            // OJO: tu ApiService aquí es Call<...>, por eso usamos execute()
            val resp = withContext(Dispatchers.IO) {
                api.getAsignacionMensual(aId).execute()
            }

            Log.d("ASIGNACION_DETALLE", "HTTP code: ${resp.code()}")
            Log.d("ASIGNACION_DETALLE", "isSuccessful: ${resp.isSuccessful}")

            if (resp.isSuccessful && resp.body() != null) {

                val body = resp.body()!!
                asignacionMensualTemp = body

                Log.d("ASIGNACION_DETALLE", "✅ Body keys: ${body.keys.joinToString(", ")}")

                // Log completo (nivel 1)
                Log.d("ASIGNACION_DETALLE", "---------- RAW BODY (level 1) ----------")
                body.forEach { (k, v) ->
                    Log.d("ASIGNACION_DETALLE", "$k = $v")
                }

                // Intentar leer plan_salud y drugs (según tu estructura)
                runCatching {
                    val plan = body["plan_salud"] as? Map<*, *>
                    Log.d("ASIGNACION_DETALLE", "---------- plan_salud ----------")
                    plan?.forEach { (k, v) ->
                        Log.d("ASIGNACION_DETALLE", "plan_salud.$k = $v")
                    }

                    // Actualiza UI (si existen)
                    val pres = (plan?.get("presupuesto_mensual") as? Number)?.toDouble()
                    if (pres != null) {
                        budget = pres
                        remainingBudget = pres
                        Log.d("ASIGNACION_DETALLE", "✅ budget set from plan_salud.presupuesto_mensual = $budget")
                    }

                    val seguro = plan?.get("seguro_medico")?.toString()
                    if (!seguro.isNullOrBlank()) {
                        insuranceName = seguro
                        Log.d("ASIGNACION_DETALLE", "✅ insuranceName set from plan_salud.seguro_medico = $insuranceName")
                    }
                }.onFailure {
                    Log.e("ASIGNACION_DETALLE", "❌ Error parsing plan_salud", it)
                }

                runCatching {
                    val drugs = body["drugs"] as? List<Map<String, Any>>
                    Log.d("ASIGNACION_DETALLE", "---------- drugs ----------")
                    Log.d("ASIGNACION_DETALLE", "drugs count: ${drugs?.size ?: 0}")

                    drugs?.take(5)?.forEachIndexed { idx, item ->
                        Log.d("ASIGNACION_DETALLE", "drug[$idx] keys: ${item.keys.joinToString(", ")}")
                        item.forEach { (k, v) ->
                            Log.d("ASIGNACION_DETALLE", "drug[$idx].$k = $v")
                        }
                    }

                    if (!drugs.isNullOrEmpty()) {
                        sections = drugs
                            .groupBy { (it["categoria"] as? Map<*, *>)?.get("nombre") ?: "Uncategorized" }
                            .map { (cat, list) ->
                                DrugSection(
                                    category = cat.toString(),
                                    items = list.map { d ->
                                        DrugItem(
                                            id = (d["id"] as Number).toInt(),
                                            code = d["code"].toString(),
                                            nombre = d["nombre"].toString(),
                                            nombremarca = d["nombremarca"].toString(),
                                            dosis = d["dosis"].toString(),
                                            tamano = d["tamano"].toString(),
                                            price = (d["costototal"] as Number).toDouble()
                                        )
                                    }
                                )
                            }

                        Log.d("ASIGNACION_DETALLE", "✅ sections created: ${sections.size}")
                        sections.forEachIndexed { idx, sec ->
                            Log.d("ASIGNACION_DETALLE", "section[$idx] category='${sec.category}' items=${sec.items.size}")
                        }
                    } else {
                        Log.e("ASIGNACION_DETALLE", "❌ drugs list is empty or null")
                    }
                }.onFailure {
                    Log.e("ASIGNACION_DETALLE", "❌ Error parsing drugs/sections", it)
                }

            } else {
                val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                Log.e("ASIGNACION_DETALLE", "❌ Backend error (${resp.code()}): ${err ?: "no error body"}")
            }

        } catch (e: Exception) {
            Log.e("ASIGNACION_DETALLE", "❌ Exception calling getAsignacionMensual()", e)
        }

        Log.d("ASIGNACION_DETALLE", "================ END ASIGNACION MENSUAL =================")
    }

    // ——— Helpers de UI/negocio ———

    fun calcularTotal(): Double =
        selected.filter { it.value }.keys.sumOf { id ->
            sections.flatMap { it.items }.first { it.id == id }.price
        }



    fun isPedidoDelMesActual(pedido: Map<String, Any>): Boolean {
        // ✅ Keys reales de tu backend
        val raw = (
                pedido["create_date"]?.toString()
                    ?: pedido["fecha_pedido"]?.toString()
                    ?: pedido["created_at"]?.toString()
                ) ?: return false

        // ✅ Si viene ISO, nos quedamos con "YYYY-MM-DD"
        val datePart = raw.take(10)

        // Validación mínima del formato
        if (datePart.length != 10 || datePart[4] != '-' || datePart[7] != '-') return false

        val year = datePart.substring(0, 4).toIntOrNull() ?: return false
        val month = datePart.substring(5, 7).toIntOrNull() ?: return false

        val cal = java.util.Calendar.getInstance()
        val currentYear = cal.get(java.util.Calendar.YEAR)
        val currentMonth = cal.get(java.util.Calendar.MONTH) + 1 // 0-based

        return year == currentYear && month == currentMonth
    }

    fun getStatus(pedido: Map<String, Any>): String? {
        return (pedido["status"] as? String) ?: (pedido["estado"] as? String)
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

//    // —— Único endpoint real de tu backend
//    suspend fun fetchPedidoEnProgreso(api: ApiService, userId: Int): Triple<Int?, String?, List<Int>>? {
//        runCatching { api.getPedidoEnProgreso(userId) }.getOrNull()?.let { resp ->
//            if (resp.isSuccessful && resp.body() != null && resp.code() !in listOf(204, 404)) {
//                return parsePedidoBody(resp.body()!!)
//            }
//        }
//        return null
//    }


    // ——— Si sections llega y había meds pendientes, aplícalas ———
    LaunchedEffect(sections) {
        pendingMeds?.let { meds ->
            if (sections.isNotEmpty()) aplicarSeleccionPrevia(meds)
        }
    }

    // ——— Recarga al volver (ON_RESUME): solo pedido/selección ———
//    DisposableEffect(lifecycleOwner) {
//        val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
//            if (event == Lifecycle.Event.ON_RESUME) {
//                scope.launch {
//                    val triple = fetchPedidoEnProgreso(api, userId)
//                    triple?.let { (pid, status, meds) ->
//                        pedidoId = pid
//                        pedidoEnProgreso = status == "P"
//                        pedidoFinalizado = status == "C"
//                        if (meds.isNotEmpty()) aplicarSeleccionPrevia(meds)
//                    }
//                }
//            }
//        }
//        lifecycleOwner.lifecycle.addObserver(observer)
//        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
//    }
    suspend fun fetchPedidoEnProgreso(api: ApiService, userId: Int): Triple<Int?, String?, List<Int>>? {
        return runCatching { api.getPedidoEnProgreso(userId) }
            .getOrNull()
            ?.let { resp ->
                Log.d("PEDIDO_FLOW", "GET en_progreso -> HTTP=${resp.code()} ok=${resp.isSuccessful} body=${resp.body()}")

                // Si tu backend devuelve 404/204 cuando no hay pedido, simplemente retorna null
                if (!resp.isSuccessful || resp.body() == null || resp.code() in listOf(204, 404)) {
                    return null
                }

                parsePedidoBody(resp.body()!!)
            }
    }


    LaunchedEffect(userId) {
        val TAG = "PEDIDO_INIT"
        val uid = userId ?: run {
            Log.e(TAG, "No userId in session, cannot check pedidos")
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        Log.d(TAG, "Checking pedidos for user_id=$uid ...")

        try {
            // 1) Primero: revisar historial para ver si ya existe un pedido COMPLETADO (C) este mes
            val historialResp = withContext(Dispatchers.IO) {
                api.getHistorial(uid).execute() // Call<List<Map<String,Any>>>
            }

            Log.d(TAG, "Historial -> HTTP=${historialResp.code()} ok=${historialResp.isSuccessful}")

            if (historialResp.isSuccessful && historialResp.body() != null) {
                val historial = historialResp.body()!!

                val completedThisMonth = historial.any { pedido ->
                    val st = getStatus(pedido)
                    st == "C" && isPedidoDelMesActual(pedido)
                }

                if (completedThisMonth) {
                    // ✅ Regla nueva: si ya hay C este mes -> NO mostrar botones ni lista
                    mesCompletado = true
                    msgMesCompletado = "✅ Ya este mes usted completó su pedido."

                    selected.clear()
                    sections = emptyList()
                    expandedCategory = null
                    pendingMeds = null


                    pedidoFinalizado = true
                    pedidoEnProgreso = false
                    pedidoId = null

                    Log.d(TAG, "✅ Found completed pedido (C) this month -> blocking UI")
                    return@LaunchedEffect
                }
            } else {
                Log.w(TAG, "Historial not available or failed. Continuing with en_progreso check...")
            }

            // 2) Si NO hay completado este mes, entonces sí buscamos si hay en progreso (P)
            val triple = withContext(Dispatchers.IO) {
                fetchPedidoEnProgreso(api, uid)
            }

            Log.d(TAG, "fetchPedidoEnProgreso result = $triple")

            if (triple != null) {
                val (pid, status, meds) = triple
                pedidoId = pid
                pedidoEnProgreso = status == "P"
                pedidoFinalizado = status == "C" // normalmente no pasa aquí, pero ok

                if (meds.isNotEmpty()) aplicarSeleccionPrevia(meds)

                Log.d(TAG, "Pedido loaded -> pedidoId=$pedidoId status=$status meds=${meds.size}")
            } else {
                Log.d(TAG, "No pedido en progreso -> show SAVE button")
                pedidoId = null
                pedidoEnProgreso = false
                pedidoFinalizado = false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading pedidos: ${e.message}", e)
            pedidoId = null
            pedidoEnProgreso = false
            pedidoFinalizado = false
        } finally {
            isLoading = false
            Log.d(TAG, "PEDIDO_INIT done -> isLoading=false")
        }
    }




    // ——— Acciones ———

    fun handleSave() {
        scope.launch {
            isLoading = true

            val TAG = "PEDIDO_FLOW"
            val ts = System.currentTimeMillis()

            Log.d(TAG, "================ HANDLE_SAVE START ================")
            Log.d(TAG, "timestamp=$ts")
            Log.d(TAG, "userId(raw nullable)=$userId")
            Log.d(TAG, "selected count=${selected.size} | selected=true count=${selected.count { it.value }}")
            Log.d(TAG, "pedidoId(current)=$pedidoId | pedidoEnProgreso=$pedidoEnProgreso | pedidoFinalizado=$pedidoFinalizado")
            Log.d(TAG, "===================================================")

            // ✅ userId obligatorio (PedidoRequest.user es Int)
            val uid = userId ?: run {
                val msg = "❌ No hay userId en sesión. Vuelve a iniciar sesión."
                Log.e(TAG, msg)
                scope.launch { snackbarHostState.showSnackbar(msg) }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                isLoading = false
                Log.e(TAG, "================ HANDLE_SAVE ABORT (NO USERID) ================")
                return@launch
            }

            // ✅ meds (en tu caso keys ya son Int, pero lo dejo como tú lo tenías)
            val meds = selected.filter { it.value }.keys.map { it.toInt() }
            val total = totalRedondeado()

            Log.d(TAG, "Preparing payload...")
            Log.d(TAG, "uid=$uid")
            Log.d(TAG, "meds(ids)=${meds.joinToString(",")}")
            Log.d(TAG, "total(redondeado)=$total")

            val payload = PedidoRequest(
                user = uid,
                medicinas = meds,
                costototal = total
            )

            Log.d(TAG, "Payload ready -> $payload")
            Log.d(TAG, "STEP 1: POST /pedidos/ (crearPedido) ...")

            withContext(Dispatchers.IO) {
                runCatching {
                    api.crearPedido(payload)
                }.onSuccess { r ->
                    Log.d(TAG, "POST response received")
                    Log.d(TAG, "HTTP=${r.code()} | isSuccessful=${r.isSuccessful}")
                    Log.d(TAG, "body=${r.body()}")

                    if (r.isSuccessful) {
                        // ✅ El backend devuelve id después de crear el pedido
                        val createdId = (r.body()?.get("id") as? Number)?.toInt()
                        Log.d(TAG, "createdId(parsed)=$createdId")

                        // ✅ Importante: LA PRIMERA VEZ SIEMPRE LO FORZAMOS A 'P'
                        // Porque tu endpoint /pedidos/en_progreso/ solo devuelve pedidos con status='P'
                        if (createdId != null) {
                            Log.d(TAG, "STEP 2: PATCH /pedidos/{id}/ -> force status='P' (en progreso)")
                            val patchResp = runCatching {
                                api.updatePedido(createdId, UpdatePedidoPayload(status = "P"))
                            }.getOrNull()

                            Log.d(TAG, "PATCH response -> " +
                                    "HTTP=${patchResp?.code()} | isSuccessful=${patchResp?.isSuccessful} | body=${patchResp?.body()}")
                        } else {
                            Log.w(TAG, "⚠️ createdId is NULL. Can't PATCH status='P'. " +
                                    "Esto puede romper el re-fetch de en_progreso si el backend requiere status='P'.")
                        }

                        // ✅ Re-fetch: ahora buscamos el pedido “real” que queda en progreso
                        // Esto es clave para:
                        // - obtener el pedidoId final (por si el backend cambia algo)
                        // - obtener medicinas asociadas (estado persistente)
                        Log.d(TAG, "STEP 3: GET /pedidos/en_progreso/?user_id=$uid (refetch)")
                        val again = runCatching { fetchPedidoEnProgreso(api, uid) }.getOrNull()

                        Log.d(TAG, "refetch result -> $again")

                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "STEP 4: Apply result to UI state (Main thread)")

                            if (again != null) {
                                val (pid2, status2, meds2) = again
                                Log.d(TAG, "Parsed from refetch -> pedidoId=$pid2 status=$status2 meds=${meds2.joinToString(",")}")

                                pedidoId = pid2
                                pedidoEnProgreso = status2 == "P"
                                pedidoFinalizado = status2 == "C"

                                // ✅ Esto mantiene el mismo estado si reabres la app
                                // (marcando las medicinas que ya estaban en el pedido)
                                if (meds2.isNotEmpty()) {
                                    Log.d(TAG, "Applying previous selection (rehydrate) -> meds2 count=${meds2.size}")
                                    aplicarSeleccionPrevia(meds2)
                                } else {
                                    Log.d(TAG, "No meds returned from refetch to apply")
                                }
                            } else {
                                // Si por alguna razón el backend no devuelve body, igual marcamos en progreso
                                Log.w(TAG, "⚠️ refetch returned null. Setting pedidoEnProgreso=true as fallback.")
                                pedidoEnProgreso = true
                            }

                            Log.d(TAG, "UI updated -> pedidoId=$pedidoId | enProgreso=$pedidoEnProgreso | finalizado=$pedidoFinalizado")

                            scope.launch { snackbarHostState.showSnackbar("✅ Se ha creado el pedido") }
                            Toast.makeText(context, "Se ha creado el pedido", Toast.LENGTH_SHORT).show()

                            Log.d(TAG, "✅ USER STORY: Ahora tenemos pedidoId y meds asociadas.")
                            Log.d(TAG, "✅ Esto permite que al reabrir la app podamos llamar GET en_progreso y recuperar el mismo pedido.")
                        }

                    } else {
                        Log.e(TAG, "❌ POST crearPedido failed. Handling backend error...")
                        withContext(Dispatchers.Main) {
                            scope.launch {
                                mostrarErrorBackend(r) { msg ->
                                    Log.e(TAG, "Backend error msg: $msg")
                                    snackbarHostState.showSnackbar(msg)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                }.onFailure { ex ->
                    Log.e(TAG, "❌ Network/Exception in crearPedido: ${ex.message}", ex)
                    withContext(Dispatchers.Main) {
                        val msg = "Error de red al guardar"
                        snackbarHostState.showSnackbar(msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            isLoading = false
            Log.d(TAG, "================ HANDLE_SAVE END ================")
            Log.d(TAG, "Final state -> pedidoId=$pedidoId | enProgreso=$pedidoEnProgreso | finalizado=$pedidoFinalizado")
            Log.d(TAG, "=================================================")
        }
    }
    fun handleUpdate() {
        val TAG = "PEDIDO_UPDATE"

        scope.launch {
            // —— Validaciones básicas ——
            val id = pedidoId ?: run {
                Log.e(TAG, "❌ pedidoId es null, no se puede actualizar")
                return@launch
            }

            val uid = userId ?: run {
                val msg = "❌ No hay userId en sesión. Vuelve a iniciar sesión."
                Log.e(TAG, msg)
                snackbarHostState.showSnackbar(msg)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                return@launch
            }

            // —— Construcción del payload ——
            val meds: List<Int> = selected
                .filter { it.value }
                .keys
                .toList()

            val total = totalRedondeado()

            val payload = UpdatePedidoPayload(
                medicinas = meds,
                costototal = total,
                status = "P" // mantener en progreso
            )

            Log.d(TAG, "Updating pedido -> id=$id userId=$uid meds=${meds.size} total=$total")

            try {
                // —— PATCH pedido ——
                val r = withContext(Dispatchers.IO) {
                    api.updatePedido(id, payload)
                }

                Log.d(
                    TAG,
                    "PATCH response -> HTTP=${r.code()} isSuccessful=${r.isSuccessful} body=${r.body()}"
                )

                if (r.isSuccessful) {
                    // —— Re-fetch pedido en progreso ——
                    val triple = withContext(Dispatchers.IO) {
                        fetchPedidoEnProgreso(api, uid)
                    }

                    Log.d(TAG, "Refetch result -> $triple")

                    triple?.let { (pid, status, medsIds) ->
                        pedidoId = pid
                        pedidoEnProgreso = status == "P"
                        pedidoFinalizado = status == "C"

                        if (medsIds.isNotEmpty()) {
                            aplicarSeleccionPrevia(medsIds)
                        }
                    }

                    snackbarHostState.showSnackbar("✅ Se ha actualizado el pedido")
                    Toast.makeText(context, "Se ha actualizado el pedido", Toast.LENGTH_SHORT).show()

                } else {
                    mostrarErrorBackend(r) { msg ->
                        snackbarHostState.showSnackbar(msg)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error actualizando pedido", e)
                val msg = "Error de red al actualizar"
                snackbarHostState.showSnackbar(msg)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun handleFinalizar() {
        val id = pedidoId ?: return
        // Tu backend finaliza con PATCH status="C"
        val payload = UpdatePedidoPayload(status = "C")
        scope.launch(Dispatchers.IO) {
            runCatching { api.updatePedido(id, payload) }.onSuccess { r ->
                if (r.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        pedidoFinalizado = true
                        pedidoEnProgreso = false
                        scope.launch { snackbarHostState.showSnackbar("✅ Pedido finalizado") }
                        Toast.makeText(context, "Pedido finalizado", Toast.LENGTH_SHORT).show()
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
                    val msg = "Error de red al finalizar"
                    snackbarHostState.showSnackbar(msg)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ——— UI ———
    // ——— UI ———
    val violet = MaterialTheme.colorScheme.primary
    val onViolet = MaterialTheme.colorScheme.onPrimary

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Assignments", color = onViolet, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = onViolet)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = violet,
                    titleContentColor = onViolet,
                    navigationIconContentColor = onViolet,
                    actionIconContentColor = onViolet
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // ✅ Resumen pro (no cambia lógica: solo usa tus variables)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    // 🔹 LINEA 1: Email + Insurance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = savedEmail ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = insuranceName.ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 🔹 LINEA 2: Budget + Remaining
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Budget: $${"%.2f".format(budget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Remaining: $${"%.2f".format(remainingBudget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

            }
                Spacer(Modifier.height(12.dp))

            // ✅ Mensaje de mes completado (igual lógica, solo mejor vista)
            if (mesCompletado) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msgMesCompletado.ifBlank { "✅ Ya este mes usted completó su pedido." },
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            } else {
                // ✅ Barra de acciones pro (misma lógica)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                        } else {
                            when {
                                pedidoFinalizado -> {
                                    Text(
                                        "✅ Tu pedido ha sido completado",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                (pedidoId != null) || pedidoEnProgreso -> {
                                    Button(
                                        onClick = { handleUpdate() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = violet)
                                    ) {
                                        Text("Update", color = onViolet, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { handleFinalizar() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text("Finalizar", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                else -> {
                                    Button(
                                        onClick = { handleSave() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = violet)
                                    ) {
                                        Text("Save", color = onViolet, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // ✅ Lista (misma lógica, mejor look)
            LazyColumn {
                sections.forEach { section ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        section.category,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (expandedCategory == section.category)
                                            Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (expandedCategory == section.category) {
                                    section.items.forEach { item ->
                                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        ListItem(
                                            leadingContent = {
                                                Checkbox(
                                                    checked = selected[item.id] == true,
                                                    onCheckedChange = { onToggle(item.id, item.price) }
                                                )
                                            },
                                            headlineContent = {
                                                Text(
                                                    "${item.code} | ${item.nombre}",
                                                    fontWeight = FontWeight.Medium
                                                )
                                            },
                                            supportingContent = {
                                                Text("${item.nombremarca} • ${item.dosis} • ${item.tamano}")
                                            },
                                            trailingContent = {
                                                Text("$${"%.2f".format(item.price)}")
                                            }
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
