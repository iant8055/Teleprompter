package com.example.teleprompter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.teleprompter.ui.theme.TeleprompterTheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

enum class AppScreen { INTRO, MAIN, SETTINGS, HOWTO }

// --- Data Models ---
class Script(
    val id: String = UUID.randomUUID().toString(),
    name: String,
    content: String,
    description: String = ""
) {
    var name by mutableStateOf(name)
    var content by mutableStateOf(content)
    var description by mutableStateOf(description) // Added field
}

class Scene(
    val id: String = UUID.randomUUID().toString(),
    name: String,
    val scripts: SnapshotStateList<Script> = mutableStateListOf(),
    description: String = ""
) {
    var name by mutableStateOf(name)
    var description by mutableStateOf(description) // Added field
}

class Project(
    val id: String = UUID.randomUUID().toString(),
    name: String,
    val scenes: SnapshotStateList<Scene> = mutableStateListOf(),
    description: String = ""
) {
    var name by mutableStateOf(name)
    var description by mutableStateOf(description) // Added field
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasSeenIntro = prefs.getBoolean("hasSeenIntro", false)

        setContent {
            TeleprompterTheme {
                var currentScreen by remember {
                    mutableStateOf(if (hasSeenIntro) AppScreen.MAIN else AppScreen.INTRO)
                }
                var currentLang by remember { mutableStateOf(prefs.getString("app_language", "en") ?: "en") }
                var appStrings by remember { mutableStateOf(AppStrings()) }

                LaunchedEffect(currentLang) {
                    appStrings = if (currentLang == "en") AppStrings()
                    else translateApp(this@MainActivity, currentLang, BuildConfig.TRANSLATE_API_KEY)
                        .getOrElse { AppStrings() }
                }

                CompositionLocalProvider(LocalAppStrings provides appStrings) {
                    when (currentScreen) {
                        AppScreen.INTRO -> IntroScreen(onFinished = {
                            currentLang = prefs.getString("app_language", "en") ?: "en"
                            currentScreen = AppScreen.MAIN
                        })
                        AppScreen.MAIN -> TeleprompterExplorer(
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                            onOpenHowTo = { currentScreen = AppScreen.HOWTO }
                        )
                        AppScreen.SETTINGS -> SettingsScreen(onBack = {
                            currentLang = prefs.getString("app_language", "en") ?: "en"
                            currentScreen = AppScreen.MAIN
                        })
                        AppScreen.HOWTO -> HowToScreen(onBack = { currentScreen = AppScreen.MAIN })
                    }
                }
            }
        }
    }
}

// --- Save/Load Logic ---
fun saveProjects(context: Context, projects: List<Project>) {
    try {
        val rootArray = JSONArray()
        projects.forEach { project ->
            val projObj = JSONObject()
            projObj.put("id", project.id)
            projObj.put("name", project.name)
            projObj.put("description", project.description) // Persistence

            val scenesArray = JSONArray()
            project.scenes.forEach { scene ->
                val sceneObj = JSONObject()
                sceneObj.put("id", scene.id)
                sceneObj.put("name", scene.name)
                sceneObj.put("description", scene.description) // Persistence

                val scriptsArray = JSONArray()
                scene.scripts.forEach { script ->
                    val scriptObj = JSONObject()
                    scriptObj.put("id", script.id)
                    scriptObj.put("name", script.name)
                    scriptObj.put("content", script.content)
                    scriptObj.put("description", script.description) // Persistence
                    scriptsArray.put(scriptObj)
                }
                sceneObj.put("scripts", scriptsArray)
                scenesArray.put(sceneObj)
            }
            projObj.put("scenes", scenesArray)
            rootArray.put(projObj)
        }
        val file = File(context.filesDir, "teleprompter_data.json")
        file.writeText(rootArray.toString())
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun loadProjects(context: Context): SnapshotStateList<Project> {
    val projects = mutableStateListOf<Project>()
    val file = File(context.filesDir, "teleprompter_data.json")
    if (!file.exists()) {
        projects.add(Project(name = "My First Project"))
        return projects
    }

    try {
        val fileContent = file.readText()
        if (fileContent.isBlank()) {
            projects.add(Project(name = "My First Project"))
            return projects
        }
        val rootArray = JSONArray(fileContent)
        for (i in 0 until rootArray.length()) {
            val projObj = rootArray.getJSONObject(i)
            val project = Project(
                id = projObj.optString("id", UUID.randomUUID().toString()),
                name = projObj.getString("name"),
                description = projObj.optString("description", "")
            )

            val scenesArray = projObj.optJSONArray("scenes") ?: JSONArray()
            for (j in 0 until scenesArray.length()) {
                val sceneObj = scenesArray.getJSONObject(j)
                val scene = Scene(
                    id = sceneObj.optString("id", UUID.randomUUID().toString()),
                    name = sceneObj.getString("name"),
                    description = sceneObj.optString("description", "")
                )

                val scriptsArray = sceneObj.optJSONArray("scripts") ?: JSONArray()
                for (k in 0 until scriptsArray.length()) {
                    val scriptObj = scriptsArray.getJSONObject(k)
                    val script = Script(
                        id = scriptObj.optString("id", UUID.randomUUID().toString()),
                        name = scriptObj.getString("name"),
                        content = scriptObj.getString("content"),
                        description = scriptObj.optString("description", "")
                    )
                    scene.scripts.add(script)
                }
                project.scenes.add(scene)
            }
            projects.add(project)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        projects.clear()
        projects.add(Project(name = "Error Loading"))
    }
    return projects
}

fun isNameDuplicate(
    newName: String,
    projects: List<Project>,
    selectedProject: Project?,
    selectedScene: Scene?
): Boolean {
    val trimmedNewName = newName.trim()
    return when {
        selectedScene != null -> selectedScene.scripts.any { it.name.trim().equals(trimmedNewName, ignoreCase = true) }
        selectedProject != null -> selectedProject.scenes.any { it.name.trim().equals(trimmedNewName, ignoreCase = true) }
        else -> projects.any { it.name.trim().equals(trimmedNewName, ignoreCase = true) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleprompterExplorer(onOpenSettings: () -> Unit = {}, onOpenHowTo: () -> Unit = {}) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val projects = remember { loadProjects(context) }

    // --- State Variables ---
    val showAddDialog = remember { mutableStateOf(false) }
    val newItemName = remember { mutableStateOf("") }
    val addDialogTitle = remember { mutableStateOf("") }
    val addAction = remember { mutableStateOf<((String) -> Unit)?>(null) }

    val showRenameDialog = remember { mutableStateOf(false) }
    val renameTargetName = remember { mutableStateOf("") }
    val originalRenameName = remember { mutableStateOf("") }
    val renameAction = remember { mutableStateOf<((String) -> Unit)?>(null) }

    val showDeleteDialog = remember { mutableStateOf(false) }
    val deleteTargetName = remember { mutableStateOf("") }
    val deleteAction = remember { mutableStateOf<(() -> Unit)?>(null) }

    // Description Dialog State
    val showDescriptionDialog = remember { mutableStateOf(false) }
    val descriptionTitle = remember { mutableStateOf("") }
    val descriptionInitialText = remember { mutableStateOf("") }
    val descriptionSaveAction = remember { mutableStateOf<((String) -> Unit)?>(null) }

    val selectedProject = remember { mutableStateOf<Project?>(null) }
    val selectedScene = remember { mutableStateOf<Scene?>(null) }
    val selectedScript = remember { mutableStateOf<Script?>(null) }
    val editorContent = remember { mutableStateOf("") }

    LaunchedEffect(selectedScript.value) {
        if (selectedScript.value != null) editorContent.value = selectedScript.value!!.content
    }

    LaunchedEffect(editorContent.value) {
        if (selectedScript.value != null) selectedScript.value!!.content = editorContent.value
    }

    // --- DIALOGS ---

    if (showDescriptionDialog.value) {
        DescriptionDialog(
            title = descriptionTitle.value,
            initialDescription = descriptionInitialText.value,
            onDismiss = { showDescriptionDialog.value = false },
            onSave = { newDesc ->
                descriptionSaveAction.value?.invoke(newDesc)
                saveProjects(context, projects)
                showDescriptionDialog.value = false
            }
        )
    }

    if (showAddDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog.value = false
                newItemName.value = ""
            },
            title = { Text(text = if (selectedScene.value != null) s.addNewScript else if (selectedProject.value != null) s.addNewScene else s.addNewProject) },
            text = {
                OutlinedTextField(
                    value = newItemName.value,
                    onValueChange = { newItemName.value = it },
                    label = { Text(s.name) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nameToCheck = newItemName.value.trim()
                        if (nameToCheck.isBlank()) {
                            Toast.makeText(context, s.nameCannotBeEmpty, Toast.LENGTH_SHORT).show()
                        } else if (isNameDuplicate(nameToCheck, projects, selectedProject.value, selectedScene.value)) {
                            Toast.makeText(context, s.nameAlreadyExists, Toast.LENGTH_SHORT).show()
                        } else {
                            when {
                                selectedScene.value != null -> selectedScene.value!!.scripts.add(Script(name = nameToCheck, content = ""))
                                selectedProject.value != null -> selectedProject.value!!.scenes.add(Scene(name = nameToCheck))
                                else -> projects.add(Project(name = nameToCheck))
                            }
                            saveProjects(context, projects)
                            showAddDialog.value = false
                            newItemName.value = ""
                        }
                    }
                ) { Text(s.add) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog.value = false
                    newItemName.value = ""
                }) { Text(s.cancel) }
            }
        )
    }

    if (showRenameDialog.value) {
        AlertDialog(
            onDismissRequest = { showRenameDialog.value = false },
            title = { Text("${s.rename} '${originalRenameName.value}'") },
            text = {
                OutlinedTextField(
                    value = renameTargetName.value,
                    onValueChange = { renameTargetName.value = it },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmedName = renameTargetName.value.trim()
                    if (trimmedName.isBlank()) {
                        Toast.makeText(context, s.nameCannotBeEmpty, Toast.LENGTH_SHORT).show()
                    } else if (trimmedName.equals(originalRenameName.value, ignoreCase = true)) {
                        showRenameDialog.value = false
                    } else if (isNameDuplicate(trimmedName, projects, selectedProject.value, selectedScene.value)) {
                        Toast.makeText(context, s.nameAlreadyExists, Toast.LENGTH_SHORT).show()
                    } else {
                        renameAction.value?.invoke(trimmedName)
                        saveProjects(context, projects)
                        showRenameDialog.value = false
                    }
                }) { Text(s.ok) }
            },
            dismissButton = {
                Button(onClick = { showRenameDialog.value = false }) { Text(s.cancel) }
            }
        )
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = { Text(s.confirmDelete) },
            text = { Text("${s.deleteQuestion} '${deleteTargetName.value}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteAction.value?.invoke()
                        saveProjects(context, projects)
                        showDeleteDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text(s.delete, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) { Text(s.cancel) }
            }
        )
    }

    BackHandler(enabled = selectedProject.value != null) {
        if (selectedScript.value != null) {
            saveProjects(context, projects)
            selectedScript.value = null
        } else if (selectedScene.value != null) {
            selectedScene.value = null
        } else if (selectedProject.value != null) {
            selectedProject.value = null
        }
    }

    Scaffold(
        topBar = {
            when {
                selectedScript.value != null -> ScriptToolbar(
                    projectName = selectedProject.value?.name ?: "",
                    sceneName = selectedScene.value?.name ?: "",
                    scriptName = selectedScript.value?.name ?: "",
                    onNavigateToProject = {
                        saveProjects(context, projects)
                        selectedScript.value = null
                        selectedScene.value = null
                        selectedProject.value = null
                    },
                    onNavigateToScene = {
                        saveProjects(context, projects)
                        selectedScript.value = null
                        selectedScene.value = null
                    },
                    onNavigateToScript = {
                        saveProjects(context, projects)
                        selectedScript.value = null
                    },
                    onLaunch = {
                        selectedScript.value!!.content = editorContent.value
                        saveProjects(context, projects)
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(context, FloatingPrompterService::class.java)
                            intent.putExtra("SCRIPT_TEXT", editorContent.value)
                            context.startForegroundService(intent)
                            (context as? ComponentActivity)?.moveTaskToBack(true)
                        }
                    },
                    launchLabel = s.launch
                )
                selectedScene.value != null -> ScriptListToolbar(
                    projectName = selectedProject.value?.name ?: "",
                    sceneName = selectedScene.value?.name ?: "",
                    onNavigateToProjects = {
                        selectedScene.value = null
                        selectedProject.value = null
                    },
                    onNavigateToScene = { selectedScene.value = null },
                    onOpenSettings = onOpenSettings,
                    onOpenHowTo = onOpenHowTo,
                    s = s
                )
                selectedProject.value != null -> SceneListToolbar(
                    projectName = selectedProject.value?.name ?: "",
                    onNavigateToProjects = { selectedProject.value = null },
                    onOpenSettings = onOpenSettings,
                    onOpenHowTo = onOpenHowTo,
                    s = s
                )
                else -> TopAppBar(
                    title = { Text(s.projects, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    actions = {
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(end = 4.dp)) {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(s.settings) },
                                    onClick = { menuExpanded = false; onOpenSettings() },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(s.howToUse) },
                                    onClick = { menuExpanded = false; onOpenHowTo() },
                                    leadingIcon = { Icon(Icons.Default.Info, null) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            }
        },
        floatingActionButton = {
            if (selectedScript.value == null) {
                FloatingActionButton(
                    onClick = {
                        val (title, action) = when {
                            selectedScene.value != null -> s.enterScriptName to { name: String ->
                                selectedScene.value!!.scripts.add(Script(name = name, content = ""))
                                Unit
                            }
                            selectedProject.value != null -> s.enterSceneName to { name: String ->
                                selectedProject.value!!.scenes.add(Scene(name = name))
                                Unit
                            }
                            else -> s.enterProjectName to { name: String ->
                                projects.add(Project(name = name))
                                Unit
                            }
                        }
                        addDialogTitle.value = title
                        addAction.value = action
                        showAddDialog.value = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.Add, "New") }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (selectedScript.value != null) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp).imePadding()) {
                    OutlinedTextField(
                        value = editorContent.value,
                        onValueChange = { editorContent.value = it },
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                        placeholder = { Text(s.typeScriptHere) },
                        singleLine = false,
                        maxLines = Int.MAX_VALUE
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val currentList = when {
                        selectedScene.value != null -> selectedScene.value!!.scripts.map { "script" to it }
                        selectedProject.value != null -> selectedProject.value!!.scenes.map { "scene" to it }
                        else -> projects.map { "project" to it }
                    }

                    items(currentList) { (_, item) ->
                        when (item) {
                            is Script -> FileIconItem(
                                name = item.name,
                                icon = painterResource(id = R.drawable.scripticon),
                                onClick = { selectedScript.value = item },
                                onRename = {
                                    originalRenameName.value = item.name
                                    renameTargetName.value = item.name
                                    renameAction.value = { newName -> item.name = newName }
                                    showRenameDialog.value = true
                                },
                                onDelete = {
                                    deleteTargetName.value = item.name
                                    deleteAction.value = { selectedScene.value!!.scripts.remove(item) }
                                    showDeleteDialog.value = true
                                },
                                onDescription = {
                                    descriptionTitle.value = "Script - ${item.name}"
                                    descriptionInitialText.value = item.description
                                    descriptionSaveAction.value = { item.description = it }
                                    showDescriptionDialog.value = true
                                }
                            )
                            is Scene -> FileIconItem(
                                name = item.name,
                                icon = painterResource(id = R.drawable.sceneicon),
                                onClick = { selectedScene.value = item },
                                onRename = {
                                    originalRenameName.value = item.name
                                    renameTargetName.value = item.name
                                    renameAction.value = { newName -> item.name = newName }
                                    showRenameDialog.value = true
                                },
                                onDelete = {
                                    deleteTargetName.value = item.name
                                    deleteAction.value = { selectedProject.value!!.scenes.remove(item) }
                                    showDeleteDialog.value = true
                                },
                                onDescription = {
                                    descriptionTitle.value = "Scene - ${item.name}"
                                    descriptionInitialText.value = item.description
                                    descriptionSaveAction.value = { item.description = it }
                                    showDescriptionDialog.value = true
                                }
                            )
                            is Project -> FileIconItem(
                                name = item.name,
                                icon = painterResource(id = R.drawable.projecticon),
                                onClick = { selectedProject.value = item },
                                onRename = {
                                    originalRenameName.value = item.name
                                    renameTargetName.value = item.name
                                    renameAction.value = { newName -> item.name = newName }
                                    showRenameDialog.value = true
                                },
                                onDelete = {
                                    deleteTargetName.value = item.name
                                    deleteAction.value = { projects.remove(item) }
                                    showDeleteDialog.value = true
                                },
                                onDescription = {
                                    descriptionTitle.value = "Project - ${item.name}"
                                    descriptionInitialText.value = item.description
                                    descriptionSaveAction.value = { item.description = it }
                                    showDescriptionDialog.value = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileIconItem(
    name: String,
    icon: Painter,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDescription: () -> Unit // New Parameter
) {
    var expanded by remember { mutableStateOf(false) }
    val s = LocalAppStrings.current

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().height(180.dp).clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = { expanded = true }, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
                Icon(Icons.Default.Menu, "Menu", tint = Color.Gray)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(s.description) },
                    onClick = {
                        expanded = false
                        onDescription()
                    },
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )
                DropdownMenuItem(
                    text = { Text(s.rename) },
                    onClick = {
                        expanded = false
                        onRename()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text(s.delete) },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                )
            }

            Column(modifier = Modifier.align(Alignment.Center).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(painter = icon, contentDescription = null, modifier = Modifier.size(120.dp), tint = Color.Unspecified)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun DescriptionDialog(
    title: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialDescription) }
    val s = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text(s.enterDescription) },
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { text = ""; onSave("") }) { Text(s.clear, color = Color.Red) }
                Button(onClick = { onSave(text) }) { Text(s.save) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}

@Composable
fun ScriptToolbar(
    projectName: String,
    sceneName: String,
    scriptName: String,
    onNavigateToProject: () -> Unit,
    onNavigateToScene: () -> Unit,
    onNavigateToScript: () -> Unit,
    onLaunch: () -> Unit,
    launchLabel: String
) {
    val launchGreen = colorResource(R.color.launch_green)
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(110.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreadcrumbNavItem(
                icon = painterResource(R.drawable.projecticon),
                name = projectName,
                dimmed = true,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProject
            )
            BreadcrumbSeparator()
            BreadcrumbNavItem(
                icon = painterResource(R.drawable.sceneicon),
                name = sceneName,
                dimmed = true,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToScene
            )
            BreadcrumbSeparator()
            BreadcrumbNavItem(
                icon = painterResource(R.drawable.scripticon),
                name = scriptName,
                dimmed = true,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToScript
            )
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = launchGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(2.dp))
                Text(launchLabel, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun BreadcrumbNavItem(
    icon: Painter,
    name: String,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    val itemAlpha = if (dimmed) 0.6f else 1f
    val fontWeight = if (dimmed) FontWeight.Normal else FontWeight.Bold
    val baseModifier = modifier
        .padding(horizontal = 2.dp, vertical = 4.dp)
        .alpha(itemAlpha)
    val finalModifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier

    Column(
        modifier = finalModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(66.dp),
            tint = Color.Unspecified
        )
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BreadcrumbSeparator() {
    Text(
        text = "›",
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
fun BreadcrumbMenuButton(onOpenSettings: () -> Unit, onOpenHowTo: () -> Unit, s: AppStrings) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(end = 4.dp)) {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, "More options")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(s.settings) },
                onClick = { menuExpanded = false; onOpenSettings() },
                leadingIcon = { Icon(Icons.Default.Settings, null) }
            )
            DropdownMenuItem(
                text = { Text(s.howToUse) },
                onClick = { menuExpanded = false; onOpenHowTo() },
                leadingIcon = { Icon(Icons.Default.Info, null) }
            )
        }
    }
}

@Composable
fun SceneListToolbar(
    projectName: String,
    onNavigateToProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHowTo: () -> Unit,
    s: AppStrings
) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(110.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreadcrumbNavItem(
                icon = painterResource(R.drawable.projecticon),
                name = projectName,
                dimmed = true,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProjects
            )
            BreadcrumbMenuButton(onOpenSettings = onOpenSettings, onOpenHowTo = onOpenHowTo, s = s)
        }
    }
}

@Composable
fun ScriptListToolbar(
    projectName: String,
    sceneName: String,
    onNavigateToProjects: () -> Unit,
    onNavigateToScene: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHowTo: () -> Unit,
    s: AppStrings
) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(110.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreadcrumbNavItem(
                icon = painterResource(R.drawable.projecticon),
                name = projectName,
                dimmed = true,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProjects
            )
            BreadcrumbSeparator()
            BreadcrumbNavItem(
                icon = painterResource(R.drawable.sceneicon),
                name = sceneName,
                dimmed = true,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToScene
            )
            BreadcrumbMenuButton(onOpenSettings = onOpenSettings, onOpenHowTo = onOpenHowTo, s = s)
        }
    }
}