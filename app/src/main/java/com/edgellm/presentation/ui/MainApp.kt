package com.edgellm.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.edgellm.features.gallery.GalleryScreen
import com.edgellm.features.gallery.GalleryViewModel
import com.edgellm.service.EdgeLLMService

data class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val navDestinations = listOf(
    NavDestination("chat", "Chat", Icons.Default.Chat),
    NavDestination("gallery", "Gallery", Icons.Default.Download),
    NavDestination("vision", "Vision", Icons.Default.Image),
    NavDestination("audio", "Audio", Icons.Default.Mic),
    NavDestination("lab", "Lab", Icons.Default.Science),
    NavDestination("skills", "Skills", Icons.Default.Extension),
    NavDestination("benchmark", "Bench", Icons.Default.Speed),
    NavDestination("settings", "Settings", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(edgeService: EdgeLLMService?) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                navDestinations.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = destination.title) },
                        label = { Text(destination.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("chat") {
                val viewModel: com.edgellm.features.chat.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                LaunchedEffect(edgeService?.currentEngine) {
                    viewModel.engineRef = edgeService?.currentEngine
                    viewModel.skillManager = edgeService?.skillManager
                }
                com.edgellm.features.chat.ChatScreen(vm = viewModel)
            }
            composable("gallery") {
                val viewModel: GalleryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                GalleryScreen(
                    viewModel = viewModel,
                    onModelSelected = { /* TODO: Show detail */ }
                )
            }
            composable("vision") {
                val viewModel: com.edgellm.features.askimage.AskImageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                LaunchedEffect(edgeService?.currentEngine) {
                    viewModel.engineRef = edgeService?.currentEngine
                }
                com.edgellm.features.askimage.AskImageScreen(viewModel = viewModel)
            }
            composable("audio") {
                val viewModel: com.edgellm.features.audioscribe.AudioScribeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                LaunchedEffect(edgeService?.currentEngine) {
                    viewModel.engineRef = edgeService?.currentEngine
                }
                com.edgellm.features.audioscribe.AudioScribeScreen(viewModel = viewModel)
            }
            composable("lab") {
                val viewModel: com.edgellm.features.promptlab.PromptLabViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                LaunchedEffect(edgeService?.currentEngine) {
                    viewModel.engineRef = edgeService?.currentEngine
                }
                com.edgellm.features.promptlab.PromptLabScreen(viewModel = viewModel)
            }
            composable("skills") {
                val viewModel: com.edgellm.features.agentskills.AgentSkillsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                LaunchedEffect(edgeService?.currentEngine, edgeService?.skillManager) {
                    viewModel.engineRef = edgeService?.currentEngine
                    viewModel.skillManager = edgeService?.skillManager
                }
                val skills by edgeService?.skillManager?.skills?.collectAsState(initial = emptyList()) ?: mutableStateOf(emptyList())
                com.edgellm.features.agentskills.AgentSkillsScreen(viewModel = viewModel, skills = skills)
            }
            composable("benchmark") {
                val viewModel: com.edgellm.features.benchmark.BenchmarkViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                LaunchedEffect(edgeService?.currentEngine) {
                    viewModel.engineRef = edgeService?.currentEngine
                }
                com.edgellm.features.benchmark.BenchmarkScreen(viewModel = viewModel)
            }
            composable("settings") {
                com.edgellm.features.settings.SettingsScreen()
            }
        }
    }
}