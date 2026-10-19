package io.github.halilozel1903.openf1.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * A race hub built on openf1-kotlin. It loads the live OpenF1 API and the drivers' headshots and countries' flags
 * with Coil. `scripts/screenshots.sh` starts it with `--es scene <scene>` to open a fixed screen of the 2025 Abu
 * Dhabi Grand Prix, loaded live like a normal launch:
 *
 * - `meetings`: the 2025 season
 * - `race`: the classification built from position data
 * - `drivers`: the driver grid with team colours and headshots
 * - `driver`: Lando Norris's race: lap times, tyre stints and pit stops
 * - `tablet`: classification and driver side by side (tablets in landscape)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            RaceHubTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    RaceHubApp(scene = scene, repository = Repositories.get(forScenes = scene != null))
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
