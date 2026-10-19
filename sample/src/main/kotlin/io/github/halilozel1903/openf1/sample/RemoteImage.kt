package io.github.halilozel1903.openf1.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.delay

/** How often a failed image is requested again before it counts as failed. */
private const val ImageRetries = 3

/**
 * An image from OpenF1 data (a driver's `headshot_url`, a meeting's `country_flag`), loaded with Coil. [placeholder]
 * shows while it loads, and stays when there is no url. A failed load is retried a few times. Screenshot scenes
 * wait until every one of these that has a url has loaded.
 */
@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholder: @Composable () -> Unit = {},
) {
    val monitor = LocalSceneMonitor.current
    var attempt by remember(url) { mutableIntStateOf(0) }
    var status by remember(url) { mutableStateOf(ImageStatus.Loading) }
    if (monitor != null && !url.isNullOrEmpty()) {
        val id = remember { Any() }
        DisposableEffect(monitor, id) { onDispose { monitor.image(id, null) } }
        // snapshotFlow starts with the current value, so a load that finished before this effect ran still counts.
        // A failure that will be retried still counts as loading.
        LaunchedEffect(monitor, id, url) {
            snapshotFlow {
                if (status == ImageStatus.Failed && attempt < ImageRetries) ImageStatus.Loading else status
            }.collect { monitor.image(id, it) }
        }
    }
    if (status == ImageStatus.Failed && !url.isNullOrEmpty() && attempt < ImageRetries) {
        LaunchedEffect(url, attempt) {
            delay(1_000L shl attempt)
            status = ImageStatus.Loading
            attempt++
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (url.isNullOrEmpty() || status != ImageStatus.Loaded) placeholder()
        if (!url.isNullOrEmpty()) {
            key(url, attempt) {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    alignment = alignment,
                    modifier = Modifier.fillMaxSize(),
                    onState = { state ->
                        status = when (state) {
                            is AsyncImagePainter.State.Success -> ImageStatus.Loaded
                            is AsyncImagePainter.State.Error -> ImageStatus.Failed
                            else -> ImageStatus.Loading
                        }
                    },
                )
            }
        }
    }
}
