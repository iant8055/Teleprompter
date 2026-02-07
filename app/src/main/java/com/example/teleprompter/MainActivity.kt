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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
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
        setContent {
            TeleprompterTheme {
                TeleprompterExplorer()
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
fun TeleprompterExplorer() {
    val context = LocalContext.current
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
            title = { Text(text = "Add New ${if (selectedScene.value != null) "Script" else if (selectedProject.value != null) "Scene" else "Project"}") },
            text = {
                OutlinedTextField(
                    value = newItemName.value,
                    onValueChange = { newItemName.value = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nameToCheck = newItemName.value.trim()
                        if (nameToCheck.isBlank()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        } else if (isNameDuplicate(nameToCheck, projects, selectedProject.value, selectedScene.value)) {
                            Toast.makeText(context, "Name already exists", Toast.LENGTH_SHORT).show()
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
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog.value = false
                    newItemName.value = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (showRenameDialog.value) {
        AlertDialog(
            onDismissRequest = { showRenameDialog.value = false },
            title = { Text("Rename '${originalRenameName.value}'") },
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
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (trimmedName.equals(originalRenameName.value, ignoreCase = true)) {
                        showRenameDialog.value = false
                    } else if (isNameDuplicate(trimmedName, projects, selectedProject.value, selectedScene.value)) {
                        Toast.makeText(context, "Name already exists", Toast.LENGTH_SHORT).show()
                    } else {
                        renameAction.value?.invoke(trimmedName)
                        saveProjects(context, projects)
                        showRenameDialog.value = false
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showRenameDialog.value = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete '${deleteTargetName.value}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteAction.value?.invoke()
                        saveProjects(context, projects)
                        showDeleteDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) { Text("Cancel") }
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
            TopAppBar(
                title = {
                    val pName = selectedProject.value?.name ?: ""
                    val sName = selectedScene.value?.name ?: ""
                    val scName = selectedScript.value?.name ?: ""
                    val titleText = when {
                        selectedScript.value != null -> "Project ($pName) Scene ($sName) Script ($scName)"
                        selectedScene.value != null -> "Project ($pName) Scene ($sName)"
                        selectedProject.value != null -> "Project ($pName)"
                        else -> "Projects"
                    }
                    Text(text = titleText, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (selectedProject.value != null) {
                        IconButton(onClick = {
                            if (selectedScript.value != null) {
                                saveProjects(context, projects)
                                selectedScript.value = null
                            } else if (selectedScene.value != null) {
                                selectedScene.value = null
                            } else {
                                selectedProject.value = null
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                },
                actions = {
                    if (selectedScript.value != null) {
                        Button(
                            onClick = {
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Launch")
                        }
                    } else {
                        SmallFloatingActionButton(
                            onClick = {
                                val intent = Intent(context, HowToActivity::class.java)
                                context.startActivity(intent)
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        ) { Text("?", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            if (selectedScript.value == null) {
                FloatingActionButton(
                    onClick = {
                        val (title, action) = when {
                            selectedScene.value != null -> "Enter Script Name" to { name: String ->
                                selectedScene.value!!.scripts.add(Script(name = name, content = ""))
                                Unit
                            }
                            selectedProject.value != null -> "Enter Scene Name" to { name: String ->
                                selectedProject.value!!.scenes.add(Scene(name = name))
                                Unit
                            }
                            else -> "Enter Project Name" to { name: String ->
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF0F0F0))) {
            if (selectedScript.value != null) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp).imePadding()) {
                    OutlinedTextField(
                        value = editorContent.value,
                        onValueChange = { editorContent.value = it },
                        modifier = Modifier.fillMaxSize().background(Color.White),
                        placeholder = { Text("Type your script here...") },
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

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(180.dp).clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = { expanded = true }, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
                Icon(Icons.Default.Menu, "Menu", tint = Color.Gray)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Description") },
                    onClick = {
                        expanded = false
                        onDescription()
                    },
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        expanded = false
                        onRename()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("Enter description...") },
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { text = ""; onSave("") }) { Text("Clear", color = Color.Red) }
                Button(onClick = { onSave(text) }) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}