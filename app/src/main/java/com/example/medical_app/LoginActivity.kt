package com.example.medical_app

import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.network.ApiClient
import com.tuapp.network.ApiService
import com.tuapp.network.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(onLoginSuccess = {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            })
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val api = ApiClient.instance.create(ApiService::class.java)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

//    var email by rememberSaveable { mutableStateOf("") }
//    var password by rememberSaveable { mutableStateOf("") }
    var email by remember { mutableStateOf("letal@gmail.com") }
    var password by remember { mutableStateOf("12345678") }

    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Logo de Pharma Express ---
        Image(
            painter = painterResource(id = R.drawable.logo), // Coloca tu logo en res/drawable/logo.png
            contentDescription = "Pharma Express Logo",
            modifier = Modifier.size(200.dp)
        )
        Text(
            text = "Pharma Express",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E88E5),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, bottom = 30.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            visualTransformation = PasswordVisualTransformation()
        )

        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                color = Color.Red,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    errorMsg = ""

                    try {
                        // 1️⃣ Buscar usuario por email
                        val userRes = withContext(Dispatchers.IO) {
                            api.getUserByEmail(email).awaitResponse()
                        }

                        if (!userRes.isSuccessful || userRes.body().isNullOrEmpty()) {
                            errorMsg = "Email no registrado"
                            return@launch
                        }

                        val userMap = userRes.body()?.firstOrNull()
                        val userId = (userMap?.get("id") as? Number)?.toInt()

                        if (userId == null) {
                            errorMsg = "No se pudo obtener el ID del usuario"
                            Log.e("LOGIN", "User map sin ID: $userMap")
                            return@launch
                        }

                        Log.d("LOGIN", "Usuario encontrado -> id=$userId email=$email")

                        // 2️⃣ Login
                        val loginRes = withContext(Dispatchers.IO) {
                            api.login(LoginRequest(email, password)).awaitResponse()
                        }

                        if (loginRes.isSuccessful) {
                            val token = loginRes.body()?.get("token") as? String ?: ""


                            sessionManager.saveSession(
                                token = token,
                                email = email,
                                userId = userId
                            )

                            Log.d("LOGIN", "Sesión guardada -> id=$userId token=${token.take(20)}...")
                            onLoginSuccess()
                        } else {
                            errorMsg = "Credenciales incorrectas"
                        }

                    } catch (e: Exception) {
                        Log.e("LOGIN", "Error: ${e.message}", e)
                        errorMsg = "Error de conexión"
                    } finally {
                        loading = false
                    }
                }
            }
        )
        {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        TextButton(
            onClick = {
                val intent = Intent(context, RegisterActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text("Registrarme")
        }
    }
}
