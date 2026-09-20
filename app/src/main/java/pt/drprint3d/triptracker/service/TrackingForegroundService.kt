package pt.drprint3d.triptracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.drprint3d.triptracker.MainActivity
import pt.drprint3d.triptracker.R
import pt.drprint3d.triptracker.data.AppDatabase
import pt.drprint3d.triptracker.data.LocationLog
import pt.drprint3d.triptracker.data.OperatingMode
import pt.drprint3d.triptracker.data.PreferencesRepository
import pt.drprint3d.triptracker.utils.FormatHelper
import pt.drprint3d.triptracker.utils.LocationHelper
import pt.drprint3d.triptracker.utils.SmsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackingForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var locationHelper: LocationHelper
    private lateinit var smsHelper: SmsHelper
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var database: AppDatabase

    private var trackingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(this)
        smsHelper = SmsHelper(this)
        prefsRepo = PreferencesRepository(this)
        database = AppDatabase.getDatabase(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopTracking()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification("A iniciar rastreio de localização...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true
        startTrackingLoop()

        return START_STICKY
    }

    private fun startTrackingLoop() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            while (_isServiceRunning.value) {
                val mode = prefsRepo.operatingMode
                val phone = prefsRepo.contactPhone
                val name = prefsRepo.contactName
                val formatType = prefsRepo.formatType
                val customTemplate = prefsRepo.customTemplate
                val intervalMinutes = prefsRepo.intervalMinutes.coerceAtLeast(1)

                val location = locationHelper.getCurrentLocation()
                val timestamp = System.currentTimeMillis()

                if (location != null) {
                    val formattedMsg = FormatHelper.formatLocationMessage(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        formatType = formatType,
                        customTemplate = customTemplate
                    )

                    var smsSent = false
                    if (mode == OperatingMode.LOOP_SMS && phone.isNotBlank()) {
                        smsSent = smsHelper.sendSms(phone, formattedMsg)
                    }

                    val logEntry = LocationLog(
                        timestamp = timestamp,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        accuracy = if (location.hasAccuracy()) location.accuracy else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        mode = mode.name,
                        recipientPhone = if (mode == OperatingMode.LOOP_SMS) phone else null,
                        recipientName = if (mode == OperatingMode.LOOP_SMS) name else null,
                        messageText = formattedMsg,
                        smsSent = smsSent
                    )

                    database.locationLogDao().insert(logEntry)

                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                    val statusText = if (mode == OperatingMode.LOOP_SMS) {
                        "SMS enviada às $timeStr para ${name.ifBlank { phone }}"
                    } else {
                        "Localização gravada às $timeStr"
                    }
                    updateNotification(statusText)
                } else {
                    updateNotification("A aguardar sinal GPS válido...")
                }

                delay(intervalMinutes * 60 * 1000L)
            }
        }
    }

    private fun stopTracking() {
        _isServiceRunning.value = false
        trackingJob?.cancel()
    }

    override fun onDestroy() {
        stopTracking()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TripTracker Serviço em Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação ativa para serviço de envio/registo de localização"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TrackingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TripTracker Ativo")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "PARAR", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    companion object {
        const val CHANNEL_ID = "triptracker_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "pt.drprint3d.triptracker.action.START"
        const val ACTION_STOP = "pt.drprint3d.triptracker.action.STOP"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
