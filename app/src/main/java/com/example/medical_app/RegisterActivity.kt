package com.example.medical_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.network.ApiClient
import com.tuapp.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterScreen(onRegisterSuccess = {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit) {
    val api = ApiClient.instance.create(ApiService::class.java)
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("ander") }
    var username by remember { mutableStateOf("ander") }
    var email by remember { mutableStateOf("ander@gmail.com") }
    var password by remember { mutableStateOf("12345678") }
    var policy by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }

    fun handleSubmit() {
        loading = true
        scope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "name" to name,
                    "username" to username,
                    "email" to email,
                    "password" to password,
                    "policy" to policy
                )
                val response = api.register(data).awaitResponse()
                if (response.isSuccessful) {
                    onRegisterSuccess()
                } else {
                    println("❌ Error al registrar: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Logo de Pharma Express ---
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Pharma Express Logo",
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "Pharma Express",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E88E5),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // --- Campos de Registro ---
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Checkbox(
                    checked = policy,
                    onCheckedChange = { policy = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2979FF))
                )
                Text("Acepto la política", modifier = Modifier.padding(start = 8.dp))
            }

            Button(
                onClick = { handleSubmit() },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
            ) {
                Text(if (loading) "Registrando..." else "Registrarme", color = Color.White)
            }
        }
    }
}
