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
                loading = true
                errorMsg = ""
                scope.launch(Dispatchers.IO) {
                    try {
                        val userRes = api.getUserByEmail(email).awaitResponse()
                        if (!userRes.isSuccessful || userRes.body().isNullOrEmpty()) {
                            errorMsg = "Email no registrado"
                            loading = false
                            return@launch
                        }

                        val loginRes = api.login(LoginRequest(email, password)).awaitResponse()
                        if (loginRes.isSuccessful) {
                            Log.d("LOGIN", "Token: ${loginRes.body()?.get("token")}")
                            loading = false
                            onLoginSuccess()
                        } else {
                            errorMsg = "Credenciales incorrectas"
                            loading = false
                        }

                    } catch (e: Exception) {
                        Log.e("LOGIN", "Error: ${e.message}", e)
                        errorMsg = "Error de conexión"
                        loading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        val context = LocalContext.current
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
