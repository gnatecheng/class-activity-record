package com.classrecord.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.classrecord.app.data.prefs.ThemeMode
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.ui.activities.ActivityDetailScreen
import com.classrecord.app.ui.activities.ActivityWizardScreen
import com.classrecord.app.ui.classprofile.ClassEditScreen
import com.classrecord.app.ui.home.HomeScreen
import com.classrecord.app.ui.home.HomeViewModel
import com.classrecord.app.ui.ledger.LedgerScreen
import com.classrecord.app.ui.members.MemberEditScreen
import com.classrecord.app.ui.members.MembersScreen
import com.classrecord.app.ui.navigation.Routes
import com.classrecord.app.ui.onboarding.OnboardingScreen
import com.classrecord.app.ui.settings.SettingsScreen
import com.classrecord.app.ui.subgroups.SubGroupEditScreen
import com.classrecord.app.ui.subgroups.SubGroupsScreen
import com.classrecord.app.ui.theme.ClassRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = LocalContext.current.applicationContext as ClassRecordApp
            val themeMode by app.container.userPrefs.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())
            val transparent = Color.Transparent.toArgb()
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    }
                )
            }
            ClassRecordTheme(darkTheme = darkTheme) {
                ClassRecordRoot()
            }
        }
    }
}

@Composable
private fun ClassRecordRoot() {
    val app = LocalContext.current.applicationContext as ClassRecordApp
    val homeVm: HomeViewModel = viewModel(factory = AppViewModelFactory(app.container))
    val homeState by homeVm.state.collectAsStateWithLifecycle()

    if (!homeState.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val start = if (homeState.needsOnboarding) Routes.ONBOARDING else Routes.HOME

    NavHost(
        navController = navController,
        startDestination = start,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = { goToWizard ->
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                    if (goToWizard) {
                        navController.navigate(Routes.WIZARD)
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeVm,
                onEditClass = { navController.navigate(Routes.CLASS_EDIT) },
                onMembers = { navController.navigate(Routes.MEMBERS) },
                onSubGroups = { navController.navigate(Routes.SUBGROUPS) },
                onNewActivity = { navController.navigate(Routes.WIZARD) },
                onOpenActivity = { id -> navController.navigate(Routes.detail(id)) },
                onLedger = { navController.navigate(Routes.LEDGER) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.CLASS_EDIT) {
            ClassEditScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.MEMBERS) {
            MembersScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.memberEdit(id)) }
            )
        }
        composable(
            route = Routes.MEMBER_EDIT,
            arguments = listOf(
                navArgument("memberId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("memberId") ?: -1L
            MemberEditScreen(
                memberId = if (id > 0) id else null,
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.SUBGROUPS) {
            SubGroupsScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.subgroupEdit(id)) }
            )
        }
        composable(
            route = Routes.SUBGROUP_EDIT,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("groupId") ?: -1L
            SubGroupEditScreen(
                groupId = if (id > 0) id else null,
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.WIZARD) {
            ActivityWizardScreen(
                onCancel = { navController.popBackStack() },
                onCreated = { activityId ->
                    navController.popBackStack()
                    navController.navigate(Routes.detail(activityId))
                }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("activityId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("activityId") ?: return@composable
            ActivityDetailScreen(
                activityId = id,
                onBack = { navController.popBackStack() },
                onOpenedNew = { newId -> navController.navigate(Routes.detail(newId)) }
            )
        }
        composable(Routes.LEDGER) {
            LedgerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
