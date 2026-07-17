package com.fieldlog.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fieldlog.app.ui.screens.AppearanceDialog
import com.fieldlog.app.ui.screens.ClockScreen
import com.fieldlog.app.ui.screens.ExpensesScreen
import com.fieldlog.app.ui.screens.JobsScreen
import com.fieldlog.app.ui.screens.SummaryScreen
import com.fieldlog.app.ui.theme.SettingsStore

private enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val title: String
) {
    CLOCK("clock", "Clock", Icons.Filled.AccessTime, "The clock"),
    JOBS("jobs", "Jobs", Icons.Filled.Work, "Jobs"),
    EXPENSES("expenses", "Money", Icons.Filled.Payments, "Expenses"),
    SUMMARY("summary", "Totals", Icons.Filled.Assessment, "Totals")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val nav = rememberNavController()

    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val runningJob by vm.runningJob.collectAsStateWithLifecycle()
    val todayEntries by vm.todayEntries.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    var showAppearance by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == currentRoute } ?: Tab.CLOCK

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(currentTab.title, style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { showAppearance = true }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Appearance",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { tab ->
                    val selected = backStack?.destination?.hierarchy?.any {
                        it.route == tab.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                // Don't stack up copies of screens as the user taps around.
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                            indicatorColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Tab.CLOCK.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            composable(Tab.CLOCK.route) {
                ClockScreen(
                    jobs = jobs,
                    running = running,
                    runningJob = runningJob,
                    todayEntries = todayEntries,
                    onClockIn = vm::clockIn,
                    onClockOut = { vm.clockOut() },
                    onDeleteEntry = vm::deleteEntry,
                    onGoToJobs = { nav.navigate(Tab.JOBS.route) }
                )
            }
            composable(Tab.JOBS.route) {
                JobsScreen(
                    jobs = jobs,
                    onSave = vm::saveJob,
                    onDelete = vm::deleteJob
                )
            }
            composable(Tab.EXPENSES.route) {
                ExpensesScreen(
                    expenses = expenses,
                    jobs = jobs,
                    onSave = vm::saveExpense,
                    onDelete = vm::deleteExpense
                )
            }
            composable(Tab.SUMMARY.route) {
                SummaryScreen(
                    period = period,
                    totals = totals,
                    onPeriodChange = vm::setPeriod,
                    onExportTime = { vm.exportTimeCsv() },
                    onExportExpenses = { vm.exportExpenseCsv() }
                )
            }
        }
    }

    if (showAppearance) {
        AppearanceDialog(
            mode = settings.mode,
            accent = settings.accent,
            onModeChange = { SettingsStore.setMode(context, it) },
            onAccentChange = { SettingsStore.setAccent(context, it) },
            onDismiss = { showAppearance = false }
        )
    }
}
