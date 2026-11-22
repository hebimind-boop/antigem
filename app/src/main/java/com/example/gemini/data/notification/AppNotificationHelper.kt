package com.example.gemini.data.notification

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gemini.MainActivity
import com.example.gemini.R
import com.example.gemini.data.updater.AppUpdateInfo
import java.lang.ref.WeakReference

/**
 * Tracks whether the user is actively viewing an Activity in LimitX or if the app
 * is currently backgrounded / the user is in another application.
 */
object AppLifecycleTracker : Application.ActivityLifecycleCallbacks {
    private var startedCount = 0
    private var resumedCount = 0
    private var currentActivityRef: WeakReference<Activity>? = null

    val isAppInForeground: Boolean
        get() = resumedCount > 0 || startedCount > 0

    val currentActivity: Activity?
        get() = currentActivityRef?.get()

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedCount++
    }

    override fun onActivityResumed(activity: Activity) {
        resumedCount++
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        resumedCount = (resumedCount - 1).coerceAtLeast(0)
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount = (startedCount - 1).coerceAtLeast(0)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

/**
 * Unified notification helper for LimitX AI completions and app update alerts.
 */
object AppNotificationHelper {
    const val CHANNEL_AI_REPLIES = "ai_replies"
    const val CHANNEL_APP_UPDATES = "app_updates"

    const val EXTRA_CHAT_ID = "EXTRA_CHAT_ID"
    const val EXTRA_SHOW_UPDATE = "EXTRA_SHOW_UPDATE"

    const val NOTIFICATION_ID_UPDATE = 20001
    private const val BASE_AI_NOTIFICATION_ID = 30000

    fun setupNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return

            // Channel for AI response completion
            if (nm.getNotificationChannel(CHANNEL_AI_REPLIES) == null) {
                val aiChannel = NotificationChannel(
                    CHANNEL_AI_REPLIES,
                    "AI Responses",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when an AI task or response completes while outside the app"
                    enableVibration(true)
                }
                nm.createNotificationChannel(aiChannel)
            }

            // Channel for LimitX app updates
            if (nm.getNotificationChannel(CHANNEL_APP_UPDATES) == null) {
                val updateChannel = NotificationChannel(
                    CHANNEL_APP_UPDATES,
                    "LimitX Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts when a new LimitX version is available"
                }
                nm.createNotificationChannel(updateChannel)
            }
        }
    }

    /**
     * Posts a system notification when an AI completion occurs and the user is outside the app.
     * Tapping the notification brings LimitX to the front and opens the specific chat.
     */
    fun postAiReplyNotification(
        context: Context,
        conversationId: String,
        title: String,
        snippet: String
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        setupNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_ID, conversationId)
        }

        val requestCode = (conversationId.hashCode() and 0x7FFFFFFF) % 10000
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifTitle = title.takeIf { it.isNotBlank() && it != "New Chat" } ?: "LimitX AI Response"
        val notifSnippet = snippet.takeIf { it.isNotBlank() } ?: "Response generation completed."

        val notification = NotificationCompat.Builder(context, CHANNEL_AI_REPLIES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notifTitle)
            .setContentText(notifSnippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifSnippet))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        return try {
            val notificationId = BASE_AI_NOTIFICATION_ID + requestCode
            manager.notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Posts a system notification when a new LimitX update is detected.
     */
    fun postUpdateNotification(
        context: Context,
        info: AppUpdateInfo
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        setupNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOW_UPDATE, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = info.changelog.firstOrNull() ?: "A new version of LimitX is ready to install."
        val fullChangelog = if (info.changelog.isNotEmpty()) {
            info.changelog.joinToString("\n") { "• $it" }
        } else {
            summary
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_APP_UPDATES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LimitX v${info.versionName} Available")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullChangelog))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return try {
            manager.notify(NOTIFICATION_ID_UPDATE, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
