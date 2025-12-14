package com.example.medical_app

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.medical_app.ui.theme.AppTheme
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

        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            AppTheme {
                HomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onLogout: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Pharma Express",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(30.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                )
            }
        },
        actions = {
            TextButton(
                onClick = onLogout,
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    "Salir",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun HomeScreen() {
    val api = ApiClient.instance.create(ApiService::class.java)
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var userData by remember { mutableStateOf(UserData()) }
    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val savedEmail = remember { sessionManager.getEmail() }
    val savedToken = remember { sessionManager.getToken() }

    LaunchedEffect(Unit) {
        Log.d("HOME_SESSION", "=========== SESSION READ ===========")
        Log.d("HOME_SESSION", "savedEmail: $savedEmail")
        Log.d("HOME_SESSION", "savedToken: ${savedToken?.take(20)}...")
        Log.d("HOME_SESSION", "isLoggedIn: ${sessionManager.isLoggedIn()}")
        Log.d("HOME_SESSION", "===================================")
    }

    LaunchedEffect(savedEmail) {
        if (savedEmail.isNullOrBlank()) {
            errorMsg = "No hay usuario guardado. Inicia sesión nuevamente."
            Log.e("HOME_API", "savedEmail is null/blank -> cannot call API")
            return@LaunchedEffect
        }

        loading = true
        errorMsg = ""

        try {
            val response = withContext(Dispatchers.IO) {
                api.getProfileAndPlanByName(savedEmail).awaitResponse()
            }

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    userData = UserData(
                        user_name = body["user_name"]?.toString() ?: "",
                        health_plan_name = body["health_plan_name"]?.toString() ?: "",
                        monthly_budget = body["monthly_budget"]?.toString() ?: "",
                        insurance_name = body["insurance_name"]?.toString() ?: ""
                    )
                } ?: run {
                    errorMsg = "Respuesta vacía del servidor."
                }
            } else {
                errorMsg = "Error ${response.code()}"
            }
        } catch (e: Exception) {
            errorMsg = "Fallo de conexión: ${e.message}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                onLogout = {
                    sessionManager.clearSession()
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(8.dp))

                // Avatar
                AsyncImage(
                    model = "https://randomuser.me/api/portraits/men/32.jpg",
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(92.dp)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable {
                        val intent = Intent(context, EditProfileActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ✅ TARJETA RESUMEN (como el mockup)
                SummaryCard(
                    budget = "${userData.monthly_budget} USD",
                    planDate = "21/09/24",
                    healthPlan = userData.health_plan_name,
                    insurance = userData.insurance_name
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Card Categories (igual, solo que se integra mejor)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Browse categories and view detailed medication information.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ✅ BOTONES GRANDES (como el mockup)
                PrimaryBigButton("View Assignments") {
                    val intent = Intent(context, AssignmentDetailActivity::class.java)
                    context.startActivity(intent)
                }

                Spacer(modifier = Modifier.height(14.dp))

                PrimaryBigButton("Assignment History") {
                    val intent = Intent(context, AssignmentHistoryActivity::class.java)
                    context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    budget: String,
    planDate: String,
    healthPlan: String,
    insurance: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(18.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(title = "Budget Mensual", value = budget)
                MetricItem(title = "Fecha Plan", value = planDate, alignEnd = true)
            }

            Divider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(title = "Health Plan", value = healthPlan)
                MetricItem(title = "Insurance", value = insurance, alignEnd = true)
            }
        }
    }
}

@Composable
private fun MetricItem(title: String, value: String, alignEnd: Boolean = false) {
    Column(
        modifier = Modifier.widthIn(min = 140.dp, max = 180.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.titleMedium, // 👈 MÁS PEQUEÑO
            fontWeight = FontWeight.SemiBold,             // 👈 menos agresivo
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun PrimaryBigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}
