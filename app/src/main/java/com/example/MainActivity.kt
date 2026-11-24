package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.core.ThemeMode
import com.example.core.ThemePreferences
import com.example.core.configureAppCheck
import com.example.data.repository.TaskRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.TaskFlowTheme
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.TaskDashboardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent BEFORE Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            val scope = rememberCoroutineScope()

            TaskFlowTheme(themeMode = themeMode) {
                AppRoot(
                    themeMode = themeMode,
                    onThemeModeChange = { nextMode ->
                        scope.launch {
                            themePreferences.setThemeMode(nextMode)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppRoot(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = auth.currentUser
    )

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val dashboardViewModel: TaskDashboardViewModel = viewModel(
                key = "dashboard_${user.uid}",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        TaskDashboardViewModel(
                            repository = TaskRepository(db, auth),
                            currentUserId = user.uid
                        )
                    }
                }
            )
            val aiViewModel: AiAssistantViewModel = viewModel(
                key = "ai_${user.uid}",
                factory = viewModelFactory {
                    initializer { AiAssistantViewModel() }
                }
            )

            DashboardScreen(
                currentUser = user,
                dashboardViewModel = dashboardViewModel,
                aiViewModel = aiViewModel,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }
    }
}
