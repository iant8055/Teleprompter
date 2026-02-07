// Ref: 02/06/2026 18:36
package com.example.teleprompter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HowToActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HowToScreen(onBack = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Use") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Teleprompter Guide", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            InstructionItem("1. Main Screen", "When you first open Teleprompter you are provided a default Project folder. To create a new PROJECT click the BLUE PLUS button in the lower right.")
            InstructionItem("2. Organization", "The Library is organized into PROJECTS > SCENES > SCRIPTS \n \nInside each PROJECT you can add multiple SCENES, within each SCENE you can write multiple SCRIPTS.  Just click the BLUE PLUS button.\nInside the SCRIPT window is where you will type or paste the text of your Script. This is what will scroll past on the screen.")
            InstructionItem("3. Editing", "You can Rename or Delete any PROJECT, SCENE or SCRIPT by tapping the hamburger menu item (three horizontal lines). Select 'Description' to add notes or context for that particular item.")
            InstructionItem("4. Launching", "To Launch the Teleprompter feature, tap the 'Launch' button inside any SCRIPT. This closes the main app and opens the Floating Overlay.")
            InstructionItem("5. The Overlay", "The overlay sits on top of your camera app. Open your camera app then LAUNCH your script.  This allows you to adjust the Overlay to suit your needs")
            InstructionItem("6. Controls", "• Play/Pause: Tap the Play button in the header.\n• Scroll Speed: Open Settings (gear icon) to adjust speed.\n• Opacity: Adjust the 'Background Opacity' slider to see through the prompter.\n• Resize: Drag the bottom-right corner grip.\n• Move: Drag the top header bar.")

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InstructionItem(title: String, body: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
    Spacer(modifier = Modifier.height(4.dp))
    Text(body, fontSize = 16.sp, lineHeight = 24.sp, color = Color.DarkGray)
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
    Spacer(modifier = Modifier.height(16.dp))
}