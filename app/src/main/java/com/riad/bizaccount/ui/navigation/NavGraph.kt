package com.riad.bizaccount.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.riad.bizaccount.ui.category.CategoryScreen
import com.riad.bizaccount.ui.dashboard.DashboardScreen
import com.riad.bizaccount.ui.report.ReportScreen
import com.riad.bizaccount.ui.search.SearchScreen
import com.riad.bizaccount.ui.settings.SettingsScreen
import com.riad.bizaccount.ui.transaction.AddEditTransactionScreen
import com.riad.bizaccount.ui.transaction.TransactionListScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val SEARCH = "search"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val TRANSACTION_LIST = "transaction_list"
    const val CATEGORIES = "categories"
    const val ADD_TRANSACTION = "add_transaction"
    const val EDIT_TRANSACTION = "edit_transaction/{id}"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.DASHBOARD, "ড্যাশবোর্ড", Icons.Default.Home),
    BottomTab(Routes.SEARCH, "খুঁজুন", Icons.Default.Search),
    BottomTab(Routes.REPORTS, "রিপোর্ট", Icons.Default.Assessment),
    BottomTab(Routes.SETTINGS, "সেটিংস", Icons.Default.Settings)
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            NavigationBar {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) },
                    onSeeAll = { navController.navigate(Routes.TRANSACTION_LIST) }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(onEdit = { id -> navController.navigate("edit_transaction/$id") })
            }
            composable(Routes.REPORTS) { ReportScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(onManageCategories = { navController.navigate(Routes.CATEGORIES) })
            }
            composable(Routes.TRANSACTION_LIST) {
                TransactionListScreen(
                    onEdit = { id -> navController.navigate("edit_transaction/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CATEGORIES) {
                CategoryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADD_TRANSACTION) {
                AddEditTransactionScreen(editId = null, onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.EDIT_TRANSACTION,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")
                AddEditTransactionScreen(editId = id, onDone = { navController.popBackStack() })
            }
        }
    }
}
