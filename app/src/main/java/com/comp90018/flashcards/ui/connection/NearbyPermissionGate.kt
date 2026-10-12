package com.comp90018.flashcards.ui.connection

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

// which permissions nearby needs depends on the android version, matching the manifest
private fun nearbyPermissions(): Array<String> =
    buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        // the manifest only declares location up to android 12
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            // fine is only declared from android 10, and it has to be asked for beside coarse
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }.toTypedArray()

private fun hasNearbyPermissions(context: Context): Boolean =
    nearbyPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Shows [content] once the nearby permissions are granted. Until then it asks for them, and if
 * they are refused it explains why and offers Allow, app settings and Back.
 */
@Composable
fun NearbyPermissionGate(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    debugViewModel: DebugViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val emulatorMode by debugViewModel.emulatorMode.collectAsState()
    var granted by remember { mutableStateOf(hasNearbyPermissions(context)) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            granted = hasNearbyPermissions(context)
        }

    // ask straight away the first time the screen opens, except in emulator mode where nearby is not used
    LaunchedEffect(Unit) { if (!granted && !emulatorMode) launcher.launch(nearbyPermissions()) }
    // coming back from app settings, check again
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasNearbyPermissions(context) }

    if (granted || emulatorMode) {
        content()
    } else {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Nearby access needed", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Team Mode needs bluetooth and nearby devices access to find the other phone",
                textAlign = TextAlign.Center,
            )
            Button(onClick = { launcher.launch(nearbyPermissions()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Allow")
            }
            TextButton(onClick = { openAppSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open settings")
            }
            TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}

// the system stops showing the permission prompt after a few refusals, so settings is the way out
private fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    context.startActivity(intent)
}
