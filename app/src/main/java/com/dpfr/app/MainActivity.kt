package com.dpfr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.dpfr.app.trigger.AppTriggerService
import com.dpfr.app.ui.DpfrTheme
import com.dpfr.app.ui.HomeScreen
import com.dpfr.app.ui.SettingsScreen
import com.dpfr.app.ui.ShapeEditorScreen
import com.dpfr.app.ui.TriggerScreen
import com.dpfr.app.util.ShortcutHelper

class MainActivity : ComponentActivity() {

    /** Bumped on every resume so screens re-check permissions after the user returns from Settings. */
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DpfrTheme {
                AppRoot(resumeTick.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
        ShortcutHelper.publish(this)
        AppTriggerService.sync(this)
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    PRANKS("Pranks", Icons.Rounded.AutoAwesome),
    SHAPES("Shapes", Icons.Rounded.Category),
    TRIGGERS("Triggers", Icons.Rounded.Bolt),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
private fun AppRoot(tick: Int) {
    var tab by rememberSaveable { mutableStateOf(Tab.PRANKS) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.PRANKS -> HomeScreen(tick)
                Tab.SHAPES -> ShapeEditorScreen()
                Tab.TRIGGERS -> TriggerScreen(tick)
                Tab.SETTINGS -> SettingsScreen(tick)
            }
        }
    }
}
