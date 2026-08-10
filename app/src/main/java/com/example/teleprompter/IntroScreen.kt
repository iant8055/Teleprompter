package com.example.teleprompter

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.pm.PackageManager
import android.util.Log
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Language(val code: String, val displayName: String)

val supportedLanguages = listOf(
    Language("en", "English"),
    Language("es", "Español"),
    Language("fr", "Français"),
    Language("de", "Deutsch"),
    Language("it", "Italiano"),
    Language("pt", "Português"),
    Language("ja", "日本語"),
    Language("ko", "한국어"),
    Language("zh", "中文"),
    Language("ar", "العربية"),
    Language("hi", "हिन्दी"),
    Language("ru", "Русский"),
    Language("nl", "Nederlands"),
    Language("sv", "Svenska"),
    Language("pl", "Polski"),
    Language("tr", "Türkçe"),
)

data class SlideStrings(
    // Slide 0
    val welcomeTitle: String    = "Welcome to PROMPT",
    val welcomeSub: String      = "Your professional scripting and delivery tool. Let's get you set up.",
    val langHint: String        = "Language auto-detected from your device",
    // Slide 1
    val slide1Title: String     = "Organize your scripts",
    val slide1Body: String      = "Use a simple hierarchy to keep everything in order.",
    val treeProject: String     = "My First Project",
    val treeScene1: String      = "Scene 1",
    val treeScript1: String     = "Opening remarks",
    val treeScript2: String     = "Main content",
    val treeScene2: String      = "Scene 2",
    val treeFooter: String      = "Projects → Scenes → Scripts",
    // Slide 2
    val slide2Title: String     = "Launch and scroll",
    val slide2Body: String      = "Open a script and tap Launch. A floating overlay appears on top of your camera app.",
    val feat1Title: String      = "Tap to pause",
    val feat1Body: String       = "Touch the overlay to stop scrolling at any time",
    val feat2Title: String      = "Resize & move",
    val feat2Body: String       = "Drag the corner grip or the header bar",
    val feat3Title: String      = "Mirror mode",
    val feat3Body: String       = "Flip text for teleprompter glass use",
    val permTitle: String       = "Permission required",
    val permBody: String        = "This app needs \"Draw Over Other Apps\" permission to display the teleprompter overlay. You'll be prompted to grant it on first launch.",
    // Slide 3
    val slide3Title: String     = "Tips and settings",
    val slide3Body: String      = "Customize the app to match your workflow.",
    val feat4Title: String      = "Scroll speed",
    val feat4Body: String       = "Adjust to your speaking pace",
    val feat5Title: String      = "Font size",
    val feat5Body: String       = "Make text comfortable to read at distance",
    val feat6Title: String      = "Language",
    val feat6Body: String       = "Change anytime from the ⋮ menu",
    // Slide 4
    val readyTitle: String      = "You're ready!",
    val readyBody: String       = "Create your first project and start scripting. This guide won't appear again — find help anytime in the ⋮ menu.",
    val getStarted: String      = "Get started",
    // Nav
    val skip: String            = "Skip",
    val next: String            = "Next",
)

private val DEFAULT = SlideStrings()

private fun SlideStrings.toList() = listOf(
    welcomeTitle, welcomeSub, langHint,
    slide1Title, slide1Body,
    treeProject, treeScene1, treeScript1, treeScript2, treeScene2, treeFooter,
    slide2Title, slide2Body,
    feat1Title, feat1Body, feat2Title, feat2Body, feat3Title, feat3Body,
    permTitle, permBody,
    slide3Title, slide3Body,
    feat4Title, feat4Body, feat5Title, feat5Body, feat6Title, feat6Body,
    readyTitle, readyBody, getStarted,
    skip, next,
)

private fun List<String>.toSlideStrings() = SlideStrings(
    welcomeTitle = this[0],  welcomeSub   = this[1],  langHint     = this[2],
    slide1Title  = this[3],  slide1Body   = this[4],
    treeProject  = this[5],  treeScene1   = this[6],
    treeScript1  = this[7],  treeScript2  = this[8],
    treeScene2   = this[9],  treeFooter   = this[10],
    slide2Title  = this[11], slide2Body   = this[12],
    feat1Title   = this[13], feat1Body    = this[14],
    feat2Title   = this[15], feat2Body    = this[16],
    feat3Title   = this[17], feat3Body    = this[18],
    permTitle    = this[19], permBody     = this[20],
    slide3Title  = this[21], slide3Body   = this[22],
    feat4Title   = this[23], feat4Body    = this[24],
    feat5Title   = this[25], feat5Body    = this[26],
    feat6Title   = this[27], feat6Body    = this[28],
    readyTitle   = this[29], readyBody    = this[30],
    getStarted   = this[31],
    skip         = this[32], next         = this[33],
)

fun packageSHA1(context: Context): String {
    return try {
        val info = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_SIGNING_CERTIFICATES
        )
        val sig = info.signingInfo?.apkContentsSigners?.getOrNull(0) ?: return ""
        MessageDigest.getInstance("SHA1").digest(sig.toByteArray())
            .joinToString("") { "%02X".format(it) }
    } catch (e: Exception) { "" }
}

suspend fun translateSlides(context: Context, targetLang: String, apiKey: String): Result<SlideStrings> {
    if (targetLang == "en") return Result.success(DEFAULT)
    return withContext(Dispatchers.IO) {
        try {
            val texts = DEFAULT.toList()
            val sb = StringBuilder("https://translation.googleapis.com/language/translate/v2?key=")
            sb.append(URLEncoder.encode(apiKey, "UTF-8"))
            sb.append("&target=").append(URLEncoder.encode(targetLang, "UTF-8"))
            sb.append("&format=text")
            texts.forEach { sb.append("&q=").append(URLEncoder.encode(it, "UTF-8")) }

            val conn = URL(sb.toString()).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val sha1 = packageSHA1(context)
            Log.d("Translate", "pkg=${context.packageName} sha1=$sha1")
            conn.setRequestProperty("X-Android-Package", context.packageName)
            conn.setRequestProperty("X-Android-Cert", sha1)

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val errBody = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                Log.e("Translate", "HTTP $code: $errBody")
                val message = runCatching {
                    JSONObject(errBody).getJSONObject("error").getString("message")
                }.getOrElse { "HTTP $code" }
                return@withContext Result.failure(Exception(message))
            }

            val arr = JSONObject(conn.inputStream.bufferedReader().readText())
                .getJSONObject("data").getJSONArray("translations")
            val translated = List(arr.length()) { i -> arr.getJSONObject(i).getString("translatedText") }
                .toSlideStrings()
            Result.success(translated)
        } catch (e: Exception) {
            Log.e("Translate", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val deviceLangCode = remember { context.resources.configuration.locales[0].language }
    var selectedLanguage by remember {
        mutableStateOf(supportedLanguages.find { it.code == deviceLangCode } ?: supportedLanguages[0])
    }
    var langExpanded by remember { mutableStateOf(false) }
    var strings by remember { mutableStateOf(DEFAULT) }
    var translating by remember { mutableStateOf(false) }
    var translateError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedLanguage) {
        if (selectedLanguage.code == "en") { strings = DEFAULT; translateError = null; return@LaunchedEffect }
        translating = true
        translateError = null
        val result = translateSlides(context, selectedLanguage.code, BuildConfig.TRANSLATE_API_KEY)
        result.onSuccess { strings = it }
            .onFailure { translateError = it.message }
        translating = false
    }

    val pagerState = rememberPagerState(pageCount = { 5 })

    val finishIntro: () -> Unit = {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("hasSeenIntro", true)
            .putString("app_language", selectedLanguage.code)
            .apply()
        onFinished()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> Slide0Logo(strings, selectedLanguage, langExpanded, translating, translateError,
                    onToggle  = { langExpanded = !langExpanded },
                    onDismiss = { langExpanded = false },
                    onSelect  = { selectedLanguage = it; langExpanded = false })
                1 -> Slide1Organize(strings, translating)
                2 -> Slide2Launch(strings, translating)
                3 -> Slide3Tips(strings, translating)
                4 -> Slide4Ready(strings, onGetStarted = finishIntro)
            }
        }

        // Dot indicators
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { i ->
                val active = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (active) 20.dp else 7.dp, height = 7.dp)
                        .clip(if (active) RoundedCornerShape(4.dp) else CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                        )
                )
            }
        }

        if (pagerState.currentPage < 4) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = finishIntro) { Text(strings.skip) }
                Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                    Text(strings.next)
                }
            }
        } else {
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
private fun Slide0Logo(
    strings: SlideStrings,
    selectedLanguage: Language,
    langExpanded: Boolean,
    translating: Boolean,
    translateError: String?,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (Language) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(110.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "PROMPT logo",
                modifier = Modifier.size(110.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(strings.welcomeTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(strings.welcomeSub, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(32.dp))
        Box {
            OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                Text(selectedLanguage.displayName)
            }
            DropdownMenu(expanded = langExpanded, onDismissRequest = onDismiss) {
                supportedLanguages.forEach { lang ->
                    DropdownMenuItem(text = { Text(lang.displayName) }, onClick = { onSelect(lang) })
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        when {
            translating -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text("Translating…", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            translateError != null -> Text(
                "Translation failed: $translateError",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.error
            )
            else -> Text(strings.langHint, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun Slide1Organize(strings: SlideStrings, loading: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("📁", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(strings.slide1Title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Text(strings.slide1Body, fontSize = 14.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("📂  ${strings.treeProject}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("    🎬  ${strings.treeScene1}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("        📄  ${strings.treeScript1}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("        📄  ${strings.treeScript2}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("    🎬  ${strings.treeScene2}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(strings.treeFooter, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
    }
}

@Composable
private fun Slide2Launch(strings: SlideStrings, loading: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("▶️", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(strings.slide2Title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Text(strings.slide2Body, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(20.dp))
        FeatureRow("👆", strings.feat1Title, strings.feat1Body)
        FeatureRow("↔️", strings.feat2Title, strings.feat2Body)
        FeatureRow("🪞", strings.feat3Title, strings.feat3Body)
        FeatureRow("⚠️", strings.permTitle, strings.permBody)
    }
}

@Composable
private fun Slide3Tips(strings: SlideStrings, loading: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("⚙️", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(strings.slide3Title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Text(strings.slide3Body, fontSize = 14.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(20.dp))
        FeatureRow("🏎️", strings.feat4Title, strings.feat4Body)
        FeatureRow("🔤", strings.feat5Title, strings.feat5Body)
        FeatureRow("🌐", strings.feat6Title, strings.feat6Body)
    }
}

@Composable
private fun Slide4Ready(strings: SlideStrings, onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🚀", fontSize = 56.sp)
        Spacer(Modifier.height(20.dp))
        Text(strings.readyTitle, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(strings.readyBody, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
        ) {
            Text(strings.getStarted, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(emoji, fontSize = 20.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(body, fontSize = 12.sp, lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}
