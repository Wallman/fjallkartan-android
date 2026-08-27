package fjallkartan.fjallkartan.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import fjallkartan.fjallkartan.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OfflineDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var idleStopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification(emptyList()))
        val repository = OfflineRegionRepository.get(this)
        repository.refresh()
        scope.launch {
            repository.regions.collectLatest { regions ->
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, notification(regions))
                val active = regions.any { it.status == OfflineStatus.Downloading }
                if (active) {
                    idleStopJob?.cancel()
                    idleStopJob = null
                } else if (idleStopJob == null) {
                    idleStopJob = launch {
                        delay(15_000)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        OfflineRegionRepository.get(this).refresh()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(regions: List<OfflineRegionSummary>): android.app.Notification {
        val active = regions.filter { it.status == OfflineStatus.Downloading }
        val completed = active.sumOf(OfflineRegionSummary::completedResources)
        val required = active.sumOf(OfflineRegionSummary::requiredResources)
        val progress = if (required > 0) (completed * 100 / required).toInt() else 0
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (active.isEmpty()) "Offline maps" else "Downloading offline map")
            .setContentText(
                if (active.isEmpty()) "Preparing download"
                else "${active.size} region${if (active.size == 1) "" else "s"} • $progress%",
            )
            .setContentIntent(launchIntent)
            .setOngoing(active.isNotEmpty())
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, required <= 0)
            .build()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Offline maps",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        private const val CHANNEL_ID = "offline-downloads"
        private const val NOTIFICATION_ID = 8062

        fun ensureRunning(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OfflineDownloadService::class.java),
            )
        }
    }
}
