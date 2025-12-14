package com.example.medical_app

import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medical_app.ui.theme.AppTheme
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
            AppTheme {
                LoginScreen(onLoginSuccess = {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                })
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val api = ApiClient.instance.create(ApiService::class.java)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // ✅ MISMA FUNCIONALIDAD (solo UI)
    var email by remember { mutableStateOf("letal@gmail.com") }
    var password by remember { mutableStateOf("12345678") }

    var errorMsg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val violet = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // ✅ ya no Center
        ) {
            Spacer(modifier = Modifier.height(50.dp)) // ✅ aire arriba

            // Logo (tu logo) tintado en violeta
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Pharma Express Logo",
                modifier = Modifier.size(160.dp),
                colorFilter = ColorFilter.tint(violet)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pharma Express",
                fontSize = 34.sp,                 // ✅ más grande
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Card suave como el mockup
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = violet,
                            cursorColor = violet
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = violet,
                            cursorColor = violet
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (errorMsg.isNotEmpty()) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Botón violeta grande
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        errorMsg = ""

                        try {
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = violet),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "Iniciar sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    val intent = Intent(context, RegisterActivity::class.java)
                    context.startActivity(intent)
                }
            ) {
                Text(
                    "Registrarme",
                    color = violet,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
