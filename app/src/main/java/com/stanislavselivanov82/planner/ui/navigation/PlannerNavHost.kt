package com.stanislavselivanov82.planner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stanislavselivanov82.planner.R
import com.stanislavselivanov82.planner.ui.calendar.CalendarScreen
import com.stanislavselivanov82.planner.ui.categories.CategoriesScreen
import com.stanislavselivanov82.planner.ui.day.DayScreen
import com.stanislavselivanov82.planner.ui.notes.NoteEditScreen
import com.stanislavselivanov82.planner.ui.notes.NotesScreen
import com.stanislavselivanov82.planner.ui.settings.SettingsScreen
import java.time.LocalDate

@Composable
fun PlannerNavHost(openDay: Long? = null, onDayConsumed: () -> Unit = {}) {
    val controller = rememberNavController()
    
    androidx.compose.runtime.LaunchedEffect(openDay) {
        if (openDay != null) { 
            controller.navigate("day/$openDay") { launchSingleTop = true }
            onDayConsumed() 
        }
    }
    
    val entry by controller.currentBackStackEntryAsState()
    
    val destinations = listOf(
        Triple(Routes.CALENDAR, R.string.calendar_title, Icons.Default.DateRange),
        Triple("day/${LocalDate.now().toEpochDay()}", R.string.notes_title, Icons.Default.Info),
        Triple("notes", R.string.notes_title, Icons.Default.Edit),
        Triple(Routes.SETTINGS, R.string.more_title, Icons.Default.Settings),
    )
    
    Scaffold(bottomBar = {
        if (entry?.destination?.route != "note/{noteId}") NavigationBar {
            destinations.forEach { (route, label, icon) ->
                NavigationBarItem(
                    selected = entry?.destination?.route == route ||
                        (route == Routes.SETTINGS && entry?.destination?.route == Routes.CATEGORIES),
                    onClick = {
                        controller.navigate(route) {
                            popUpTo(controller.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }) { padding ->
        NavHost(controller, startDestination = Routes.CALENDAR, modifier = Modifier.padding(padding)) {
            composable(Routes.CALENDAR) { CalendarScreen(onOpenDay = { controller.navigate("day/$it") }) }
            composable("day/{epochDay}", arguments = listOf(navArgument("epochDay") { type = NavType.LongType })) {
                DayScreen(onBack = { controller.popBackStack() })
            }
            composable("notes") { NotesScreen(onOpenNote = { controller.navigate("note/$it") }) }
            composable("note/{noteId}", arguments = listOf(navArgument("noteId") { type = NavType.LongType })) {
                NoteEditScreen(onBack = { controller.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onCategories = { controller.navigate(Routes.CATEGORIES) { launchSingleTop = true } })
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(onBack = { controller.popBackStack() })
            }
        }
    }
}
