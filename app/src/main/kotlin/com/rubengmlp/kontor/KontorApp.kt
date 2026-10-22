package com.rubengmlp.kontor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rubengmlp.kontor.feature.accounts.AccountsRoute
import com.rubengmlp.kontor.feature.accounts.TransactionDetailRoute
import com.rubengmlp.kontor.feature.accounts.TransactionsRoute
import com.rubengmlp.kontor.feature.profile.AboutScreen
import com.rubengmlp.kontor.feature.profile.ProfileRoute
import com.rubengmlp.kontor.feature.transfer.TransferRoute

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Home, "Home", Icons.Outlined.Home),
    TopLevelDestination(ActivityRoute, "Activity", Icons.AutoMirrored.Outlined.ReceiptLong),
    TopLevelDestination(TransferDestination, "Transfer", Icons.Outlined.SwapHoriz),
    TopLevelDestination(ProfileDestination, "Profile", Icons.Outlined.Person),
)

@Composable
fun KontorApp(onLock: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTopLevel = topLevelDestinations.firstOrNull { destination ->
        currentDestination.isOn(destination)
    }

    BackHandler(enabled = currentTopLevel != null && currentTopLevel.route != Home) {
        navController.popBackStack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTopLevel != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    topLevelDestinations.forEach { destination ->
                        val selected = currentTopLevel.route == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Home> {
                AccountsRoute(
                    onSeeAllTransactions = { navController.navigate(ActivityRoute) },
                    onTransactionClick = { id -> navController.navigate(TransactionDetail(id)) },
                    onTransfer = { navController.navigate(TransferDestination) },
                )
            }
            composable<ActivityRoute> {
                TransactionsRoute(
                    onTransactionClick = { id -> navController.navigate(TransactionDetail(id)) },
                )
            }
            composable<TransactionDetail> {
                TransactionDetailRoute(onBack = { navController.popBackStack() })
            }
            composable<TransferDestination> {
                TransferRoute(
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(Home) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<ProfileDestination> {
                ProfileRoute(
                    onLock = onLock,
                    onAbout = { navController.navigate(AboutDestination) },
                )
            }
            composable<AboutDestination> {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
