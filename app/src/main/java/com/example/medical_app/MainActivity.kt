package com.example.medical_app

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuapp.network.ApiClient
import com.tuapp.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

data class UserData(
    val user_name: String = "",
    val health_plan_name: String = "",
    val monthly_budget: String = "",
    val insurance_name: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Si NO hay token, manda a Login
        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            HomeScreen()
        }
    }
}

@Composable
fun HomeScreen() {
    val api = ApiClient.instance.create(ApiService::class.java)
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var userData by remember { mutableStateOf(UserData()) }
    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // ✅ Traer el valor real guardado (lo que antes era "letal")
    val savedEmail = remember { sessionManager.getEmail() } // lo guardaste en Login
    val savedToken = remember { sessionManager.getToken() }

    // ✅ LOG: sesión (solo una vez por composición)
    LaunchedEffect(Unit) {
        Log.d("HOME_SESSION", "=========== SESSION READ ===========")
        Log.d("HOME_SESSION", "savedEmail: $savedEmail")
        Log.d("HOME_SESSION", "savedToken: ${savedToken?.take(20)}...")
        Log.d("HOME_SESSION", "isLoggedIn: ${sessionManager.isLoggedIn()}")
        Log.d("HOME_SESSION", "===================================")
    }

    // Llamada a la API
    LaunchedEffect(savedEmail) {
        if (savedEmail.isNullOrBlank()) {
            errorMsg = "No hay usuario guardado. Inicia sesión nuevamente."
            Log.e("HOME_API", "savedEmail is null/blank -> cannot call API")
            return@LaunchedEffect
        }

        loading = true
        errorMsg = ""

        try {
            Log.d("HOME_API", "=========== CALLING API ===========")
            Log.d("HOME_API", "Param (savedEmail): $savedEmail")
            Log.d("HOME_API", "==================================")

            val response = withContext(Dispatchers.IO) {
                // 🔥 Aquí sustituimos "letal" por el usuario real guardado
                api.getProfileAndPlanByName(savedEmail).awaitResponse()
            }

            // ✅ LOG: respuesta cruda
            Log.d("HOME_API", "=========== API RESPONSE ===========")
            Log.d("HOME_API", "HTTP Code: ${response.code()}")
            Log.d("HOME_API", "isSuccessful: ${response.isSuccessful}")
            Log.d("HOME_API", "Body: ${response.body()}")
            Log.d("HOME_API", "====================================")

            if (response.isSuccessful) {
                response.body()?.let { body ->

                    // ✅ LOG: campos específicos
                    Log.d("HOME_API", "Parsed user_name: ${body["user_name"]}")
                    Log.d("HOME_API", "Parsed health_plan_name: ${body["health_plan_name"]}")
                    Log.d("HOME_API", "Parsed monthly_budget: ${body["monthly_budget"]}")
                    Log.d("HOME_API", "Parsed insurance_name: ${body["insurance_name"]}")

                    userData = UserData(
                        user_name = body["user_name"]?.toString() ?: "",
                        health_plan_name = body["health_plan_name"]?.toString() ?: "",
                        monthly_budget = body["monthly_budget"]?.toString() ?: "",
                        insurance_name = body["insurance_name"]?.toString() ?: ""
                    )

                    Log.d("HOME_API", "✅ userData state updated: $userData")
                } ?: run {
                    errorMsg = "Respuesta vacía del servidor."
                    Log.e("HOME_API", "Response successful but body is null")
                }
            } else {
                errorMsg = "Error ${response.code()}"
                Log.e("HOME_API", "Response NOT successful. Code=${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("HOME_API", "Exception: ${e.message}", e)
            errorMsg = "Fallo de conexión: ${e.message}"
        } finally {
            loading = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- NAV BAR SUPERIOR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1976D2))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(56.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )

                Text(
                    text = "Pharma Express",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                TextButton(onClick = {
                    // ✅ Logout: borrar sesión y volver a login
                    sessionManager.clearSession()
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Text(
                        "Salir",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // --- CONTENIDO ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(20.dp))

                // Avatar
                AsyncImage(
                    model = "https://randomuser.me/api/portraits/men/32.jpg",
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Nombre usuario
                Text(
                    text = when {
                        loading -> "Cargando..."
                        userData.user_name.isNotEmpty() -> "Hola ${userData.user_name}"
                        else -> "Hola"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A237E),
                    modifier = Modifier.clickable {
                        val intent = Intent(context, EditProfileActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = errorMsg, color = Color.Red, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoBlock("Budget Mensual", "${userData.monthly_budget} USD")
                    InfoBlock("Fecha Plan", "21/09/24")
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoBlock("Health Plan", userData.health_plan_name)
                    InfoBlock("Insurance", userData.insurance_name)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(context, CategoryListActivity::class.java)
                            context.startActivity(intent)
                        }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Categories & Medications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Browse categories and view detailed medication information.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                BlueButton("View Assignments") {
                    val intent = Intent(context, AssignmentDetailActivity::class.java)
                    context.startActivity(intent)
                }
                BlueButton("Assignment History") {
                    val intent = Intent(context, AssignmentHistoryActivity::class.java)
                    context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun InfoBlock(label: String, value: String) {
    Column(Modifier.width(140.dp), horizontalAlignment = Alignment.Start) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun BlueButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
        shape = RoundedCornerShape(25.dp)
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
