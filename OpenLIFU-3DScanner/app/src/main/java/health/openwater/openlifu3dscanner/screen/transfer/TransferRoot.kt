package health.openwater.openlifu3dscanner.screen.transfer


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/**
 * Broadcast actions the PC application sends via ADB to update transfer status.
 *
 * Usage from PC:
 *   adb shell am broadcast -a health.openwater.openlifu3dscanner.TRANSFER_STARTED  -p health.openwater.openlifu3dscanner
 *   adb shell am broadcast -a health.openwater.openlifu3dscanner.TRANSFER_COMPLETE -p health.openwater.openlifu3dscanner
 */
const val ACTION_TRANSFER_STARTED  = "health.openwater.openlifu3dscanner.TRANSFER_STARTED"
const val ACTION_TRANSFER_COMPLETE = "health.openwater.openlifu3dscanner.TRANSFER_COMPLETE"

enum class TransferStatus { NOT_STARTED, IN_PROGRESS, COMPLETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferRoot(
    collectionName: String,
) {
    val context = LocalContext.current
    val usbConnected by rememberUsbConnectionState()
    val transferStatus by rememberTransferStatus()

    LaunchedEffect(transferStatus) {
        if (transferStatus == TransferStatus.COMPLETE) {
            withContext(Dispatchers.IO) {
                getModelsDir(context).resolve(collectionName).deleteRecursively()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = stringResource(R.string.subject_scan_id_s, collectionName),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Connect USB Message
        Text(
            text = stringResource(R.string.connect_to_pc_via_usb_to_transfer_files),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(50.dp))

        // USB Icon
        Box(
            modifier = Modifier.size(width = 150.dp, height = 250.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.usb_connected_image),
                contentDescription = stringResource(if (usbConnected) R.string.usb_connected else R.string.usb_disconnected),
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (usbConnected) 1f else 0.1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Transfer status
        when (transferStatus) {
            TransferStatus.NOT_STARTED -> {
                Text(
                    text = stringResource(R.string.transfer_status_not_started),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            TransferStatus.IN_PROGRESS -> {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.transfer_status_in_progress),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            TransferStatus.COMPLETE -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.transfer_status_complete),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
fun rememberTransferStatus(): State<TransferStatus> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(TransferStatus.NOT_STARTED) }

    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(ACTION_TRANSFER_STARTED)
            addAction(ACTION_TRANSFER_COMPLETE)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                state.value = when (intent?.action) {
                    ACTION_TRANSFER_STARTED  -> TransferStatus.IN_PROGRESS
                    ACTION_TRANSFER_COMPLETE -> TransferStatus.COMPLETE
                    else -> state.value
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return state
}

fun isUsbPluggedIn(context: Context): Boolean {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    return plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_AC
}

@Composable
fun rememberUsbConnectionState(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(isUsbPluggedIn(context)) }

    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    state.value = isUsbPluggedIn(context)
                }
            }
        }

        context.registerReceiver(receiver, filter)

        // Safety watchdog polling (covers missing broadcasts)
        val polling = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                state.value = isUsbPluggedIn(context)
                delay(1.seconds.inWholeMilliseconds)
            }
        }

        onDispose {
            context.unregisterReceiver(receiver)
            polling.cancel()
        }
    }

    return state
}
