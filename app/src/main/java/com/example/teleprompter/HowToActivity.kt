// Ref: 02/06/2026 18:36
package com.example.teleprompter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val s = LocalAppStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.howToScreenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = s.howToTipLabel,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            HowToSection(
                sectionTitle = s.sectionMainScreen,
                items = listOf(
                    s.howToOverviewTitle to s.howToOverviewBody,
                    s.howToMultiLangTitle to s.howToMultiLangBody
                )
            )

            HowToSection(
                sectionTitle = s.sectionOrganization,
                items = listOf(
                    s.howToNavTitle to s.howToNavBody
                )
            )

            HowToSection(
                sectionTitle = s.sectionEditing,
                items = listOf(
                    s.howToAddTitle to s.howToAddBody,
                    s.howToRenameTitle to s.howToRenameBody,
                    s.howToDeleteTitle to s.howToDeleteBody,
                    s.howToDescTitle to s.howToDescBody
                )
            )

            HowToSection(
                sectionTitle = s.sectionLaunching,
                items = listOf(
                    s.howToPlayTitle to s.howToPlayBody,
                    s.howToSettingsTitle to s.howToSettingsBody,
                    s.howToLoopTitle to s.howToLoopBody,
                    s.howToCountdownTitle to s.howToCountdownBody
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HowToSection(sectionTitle: String, items: List<Pair<String, String>>) {
    Text(
        text = sectionTitle.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            items.forEachIndexed { index, (title, body) ->
                HowToAccordionItem(title = title, body = body)
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun HowToAccordionItem(title: String, body: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (expanded) "▾" else "›",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}