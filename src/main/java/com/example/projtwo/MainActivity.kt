// Gentian Hoxha
// 12/18/2024
// CS-360
// Project Three
package com.example.projtwo

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.projtwo.ui.theme.ProjTwoTheme

// SQLite Helper for managing the database
class DatabaseHelper(context: android.content.Context) : SQLiteOpenHelper(context, "AppDatabase", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE Users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, password TEXT)")
        db.execSQL("CREATE TABLE Data (id INTEGER PRIMARY KEY AUTOINCREMENT, item TEXT, value TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Users")
        db.execSQL("DROP TABLE IF EXISTS Data")
        onCreate(db)
    }
}

// MainActivity serves as the entry point for the application
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjTwoTheme {
                AppNavigation()
            }
        }
    }
}

// Manages navigation between different screens in the app
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("Login") }

    when (currentScreen) {
        "Login" -> LoginScreen { newScreen -> currentScreen = newScreen }
        "DataGrid" -> DataGridScreen { newScreen -> currentScreen = newScreen }
        "SMS" -> SMSPermissionScreen { newScreen -> currentScreen = newScreen }
    }
}

// Login screen with database validation
@Composable
fun LoginScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val dbHelper = DatabaseHelper(context)

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = loginMessage, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery(
                    "SELECT * FROM Users WHERE username=? AND password=?",
                    arrayOf(username, password)
                )
                if (cursor.moveToFirst()) {
                    onNavigate("DataGrid")
                } else {
                    loginMessage = "Invalid credentials. Please try again or create a new account."
                }
                cursor.close()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("username", username)
                    put("password", password)
                }
                db.insert("Users", null, values)
                loginMessage = "Account created successfully!"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }
    }
}

// Data grid screen
@Composable
fun DataGridScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val dbHelper = DatabaseHelper(context)

    var dataItems by remember { mutableStateOf(listOf<String>()) }

    fun loadData() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Data", null)
        val items = mutableListOf<String>()
        while (cursor.moveToNext()) {
            items.add("${cursor.getString(1)}: ${cursor.getString(2)}")
        }
        dataItems = items
        cursor.close()
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Data Grid", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        dataItems.forEach { item ->
            Text(item)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("item", "SampleItem")
                    put("value", "SampleValue")
                }
                db.insert("Data", null, values)
                loadData()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Data")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("SMS") }, modifier = Modifier.fillMaxWidth()) {
            Text("Request SMS Permission")
        }
    }
}

// SMS permission screen
@Composable
fun SMSPermissionScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (hasPermission) {
            Text("SMS Permission Granted")
        } else {
            Button(
                onClick = {
                    ActivityCompat.requestPermissions(
                        context as ComponentActivity,
                        arrayOf(Manifest.permission.SEND_SMS),
                        1
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request SMS Permission")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onNavigate("DataGrid") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Data Grid")
        }
    }
}

