package com.example.teleprompter

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val s = LocalAppStrings.current

    val savedCode = remember { prefs.getString("app_language", "en") ?: "en" }
    var selectedLanguage by remember {
        mutableStateOf(supportedLanguages.find { it.code == savedCode } ?: supportedLanguages[0])
    }
    var langExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(s.language, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(onClick = { langExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedLanguage.displayName)
                }
                DropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                    supportedLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.displayName) },
                            onClick = {
                                selectedLanguage = lang
                                langExpanded = false
                                prefs.edit().putString("app_language", lang.code).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
