package com.example.teleprompter

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AppStrings(
    // TopBar / nav
    val projects: String           = "Projects",
    val launch: String             = "Launch",
    val settings: String           = "Settings",
    val howToUse: String           = "How to use",
    // Add dialog
    val addNewProject: String      = "Add New Project",
    val addNewScene: String        = "Add New Scene",
    val addNewScript: String       = "Add New Script",
    val enterProjectName: String   = "Enter Project Name",
    val enterSceneName: String     = "Enter Scene Name",
    val enterScriptName: String    = "Enter Script Name",
    val name: String               = "Name",
    val add: String                = "Add",
    val cancel: String             = "Cancel",
    // Rename / delete
    val rename: String             = "Rename",
    val ok: String                 = "OK",
    val confirmDelete: String      = "Confirm Delete",
    val deleteQuestion: String     = "Are you sure you want to delete",
    val delete: String             = "Delete",
    // Description dialog
    val description: String        = "Description",
    val enterDescription: String   = "Enter description...",
    val clear: String              = "Clear",
    val save: String               = "Save",
    // Toasts
    val nameCannotBeEmpty: String  = "Name cannot be empty",
    val nameAlreadyExists: String  = "Name already exists",
    // Editor
    val typeScriptHere: String     = "Type your script here...",
    // Settings
    val language: String           = "Language",
    // How to Use screen
    val howToScreenTitle: String         = "How to Use",
    val howToTipLabel: String            = "Tap any item to expand • Tap again to close",
    // Main Screen
    val sectionMainScreen: String        = "Main Screen",
    val howToOverviewTitle: String       = "Overview",
    val howToOverviewBody: String        = "A specialized, lightweight teleprompter application designed for Android devices. This tool features a floating overlay service, allowing content creators to read scripts while simultaneously recording via the camera app or streaming live. \n\n",
    val howToMultiLangTitle: String      = "Multi-language Support",
    val howToMultiLangBody: String       = "The app interface can be displayed in multiple languages. Tap the three-dot menu on the main screen and select Settings, then choose your preferred language from the dropdown list.\n\nThe entire app — menus, buttons, and labels — will update to your chosen language automatically.",
    // Organization
    val sectionOrganization: String      = "Organization",
    val howToNavTitle: String            = "Navigation",
    val howToNavBody: String             = "When you first open Teleprompter you are provided a default Project folder. Tap the blue + button in the lower right to create a new Project.\n\nThe library is organized into Projects › Scenes › Scripts. Each screen has a breadcrumb toolbar at the top — tap any icon to jump back to that level.",
    // Editing
    val sectionEditing: String           = "Editing",
    val howToAddTitle: String            = "Add",
    val howToAddBody: String             = "Tap the blue + button on any screen to add a new Project, Scene, or Script.",
    val howToRenameTitle: String         = "Rename",
    val howToRenameBody: String          = "Tap the ≡ menu on any item and select Rename.",
    val howToDeleteTitle: String         = "Delete",
    val howToDeleteBody: String          = "Tap the ≡ menu on any item and select Delete. You'll be asked to confirm before anything is removed.",
    val howToDescTitle: String           = "Description",
    val howToDescBody: String            = "Tap the ≡ menu and select Description to add notes or context to any Project, Scene, or Script.",
    // Launching
    val sectionLaunching: String         = "Launching",
    val howToPlayTitle: String           = "Play / Pause",
    val howToPlayBody: String            = "Tap the Play button in the overlay header to start scrolling. Tap again to pause. Manually dragging the script also pauses playback.",
    val howToSettingsTitle: String       = "Settings",
    val howToSettingsBody: String        = "Tap the gear icon to open the Settings panel. Use sliders to adjust Font Size, Speed, and Opacity. Choose a text color from the color row. Toggle Mirror, and set a Countdown timer before scrolling begins.",
    val howToLoopTitle: String           = "Loop",
    val howToLoopBody: String            = "Enable Loop in the overlay Settings panel to replay your script continuously without stopping.\n\nWhen Loop is on, a duplicate of the script is shown below the original. As you reach the end, the scroll position resets seamlessly so the text flows back from the top without any jump.",
    val howToCountdownTitle: String      = "Countdown Timer",
    val howToCountdownBody: String       = "Set a Countdown Timer before your script starts scrolling. Open Settings in the overlay and choose from Off, 3, 5, or 10 seconds.\n\nWhen you tap Play, a full-screen clock animation counts down with a beep each second. At zero it flashes 'ACTION!' then scrolling begins automatically. Tap the screen at any time to cancel the countdown.",
)

val LocalAppStrings = staticCompositionLocalOf { AppStrings() }

private val DEFAULT_APP = AppStrings()
private val appStringsCache = HashMap<String, AppStrings>()

private fun AppStrings.toList() = listOf(
    projects, launch, settings, howToUse,
    addNewProject, addNewScene, addNewScript,
    enterProjectName, enterSceneName, enterScriptName,
    name, add, cancel, rename, ok,
    confirmDelete, deleteQuestion, delete,
    description, enterDescription, clear, save,
    nameCannotBeEmpty, nameAlreadyExists, typeScriptHere,
    language,
    howToTipLabel,
    sectionMainScreen, howToOverviewTitle, howToOverviewBody,
    howToMultiLangTitle, howToMultiLangBody,
    sectionOrganization, howToNavTitle, howToNavBody,
    sectionEditing, howToAddTitle, howToAddBody,
    howToRenameTitle, howToRenameBody,
    howToDeleteTitle, howToDeleteBody,
    howToDescTitle, howToDescBody,
    sectionLaunching, howToPlayTitle, howToPlayBody,
    howToSettingsTitle, howToSettingsBody,
    howToLoopTitle, howToLoopBody,
    howToCountdownTitle, howToCountdownBody,
)

private fun List<String>.toAppStrings() = AppStrings(
    projects          = this[0],  launch            = this[1],
    settings          = this[2],  howToUse          = this[3],
    addNewProject     = this[4],  addNewScene       = this[5],
    addNewScript      = this[6],  enterProjectName  = this[7],
    enterSceneName    = this[8],  enterScriptName   = this[9],
    name              = this[10], add               = this[11],
    cancel            = this[12], rename            = this[13],
    ok                = this[14], confirmDelete     = this[15],
    deleteQuestion    = this[16], delete            = this[17],
    description       = this[18], enterDescription  = this[19],
    clear             = this[20], save              = this[21],
    nameCannotBeEmpty = this[22], nameAlreadyExists = this[23],
    typeScriptHere    = this[24], language          = this[25],
    howToTipLabel         = this[26],
    sectionMainScreen     = this[27],
    howToOverviewTitle    = this[28], howToOverviewBody    = this[29],
    howToMultiLangTitle   = this[30], howToMultiLangBody   = this[31],
    sectionOrganization   = this[32],
    howToNavTitle         = this[33], howToNavBody         = this[34],
    sectionEditing        = this[35],
    howToAddTitle         = this[36], howToAddBody         = this[37],
    howToRenameTitle      = this[38], howToRenameBody      = this[39],
    howToDeleteTitle      = this[40], howToDeleteBody      = this[41],
    howToDescTitle        = this[42], howToDescBody        = this[43],
    sectionLaunching      = this[44],
    howToPlayTitle        = this[45], howToPlayBody        = this[46],
    howToSettingsTitle    = this[47], howToSettingsBody    = this[48],
    howToLoopTitle        = this[49], howToLoopBody        = this[50],
    howToCountdownTitle   = this[51], howToCountdownBody   = this[52],
)

suspend fun translateApp(context: Context, targetLang: String, apiKey: String): Result<AppStrings> {
    if (targetLang == "en") return Result.success(DEFAULT_APP)
    appStringsCache[targetLang]?.let { return Result.success(it) }
    return withContext(Dispatchers.IO) {
        try {
            val texts = DEFAULT_APP.toList()
            val sb = StringBuilder("https://translation.googleapis.com/language/translate/v2?key=")
            sb.append(URLEncoder.encode(apiKey, "UTF-8"))
            sb.append("&target=").append(URLEncoder.encode(targetLang, "UTF-8"))
            sb.append("&format=text")
            texts.forEach { sb.append("&q=").append(URLEncoder.encode(it, "UTF-8")) }

            val sha1 = packageSHA1(context)
            Log.d("TranslateApp", "pkg=${context.packageName} sha1=$sha1 strings=${texts.size} target=$targetLang")
            val conn = URL(sb.toString()).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("X-Android-Package", context.packageName)
            conn.setRequestProperty("X-Android-Cert", sha1)

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val errBody = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                Log.e("TranslateApp", "HTTP $code: $errBody")
                val message = runCatching {
                    JSONObject(errBody).getJSONObject("error").getString("message")
                }.getOrElse { "HTTP $code" }
                return@withContext Result.failure(Exception(message))
            }

            val arr = JSONObject(conn.inputStream.bufferedReader().readText())
                .getJSONObject("data").getJSONArray("translations")
            val translated = List(arr.length()) { i ->
                arr.getJSONObject(i).getString("translatedText")
            }.toAppStrings()
            appStringsCache[targetLang] = translated
            Result.success(translated)
        } catch (e: Exception) {
            Log.e("TranslateApp", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
