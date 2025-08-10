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
import kotlinx.coroutines.launch
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
        setContent {
            HomeScreen()
        }
    }
}
@Composable
fun HomeScreen() {
    val api = ApiClient.instance.create(ApiService::class.java)
    val scope = rememberCoroutineScope()
    var userData by remember { mutableStateOf(UserData()) }
    var errorMsg by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Llamada a la API
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = api.getProfileAndPlanByName("letal").awaitResponse()
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        userData = UserData(
                            user_name = body["user_name"]?.toString() ?: "",
                            health_plan_name = body["health_plan_name"]?.toString() ?: "",
                            monthly_budget = body["monthly_budget"]?.toString() ?: "",
                            insurance_name = body["insurance_name"]?.toString() ?: ""
                        )
                    }
                } else {
                    errorMsg = "Error ${response.code()}"
                }
            } catch (e: Exception) {
                errorMsg = "Fallo de conexión: ${e.message}"
            }
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
                    colorFilter = ColorFilter.tint(Color.White) // <- Lo hace blanco
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
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Text("Salir", color = Color.White,  fontSize = 20.sp,fontWeight = FontWeight.SemiBold)
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
                    text = if (userData.user_name.isNotEmpty()) "Hola ${userData.user_name}" else "Cargando...",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A237E) ,
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

                // BLOQUES DE INFORMACIÓN - TÍTULO ARRIBA, VALOR ABAJO
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoBlock("Budget Mensual", "${userData.monthly_budget} USD")
                    InfoBlock("Fecha Plan", "21/09/24")
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoBlock("Health Plan", userData.health_plan_name)
                    InfoBlock("Insurance", userData.insurance_name)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Card Categorías
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

