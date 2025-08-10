package com.example.medical_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EditProfileScreen(onBack = { finish() })
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("Jorge Pérez") }
    var email by remember { mutableStateOf("jorge.perez@example.com") }
    var phone by remember { mutableStateOf("555-123-4567") }
    var organization by remember { mutableStateOf("City Hospital") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = Color.White, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                backgroundColor = Color(0xFF2979FF),
                elevation = 4.dp
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Imagen de perfil
            AsyncImage(
                model = "https://randomuser.me/api/portraits/men/31.jpg",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card con campos de edición
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFF9F9F9),
                elevation = 2.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    ProfileField("Name", name) { name = it }
                    ProfileField("Email", email) { email = it }
                    ProfileField("Phone", phone) { phone = it }
                    ProfileField("Organization", organization) { organization = it }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Botón para guardar
            Button(
                onClick = { /* Lógica para guardar cambios */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2979FF)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Save", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
