package com.suyashsrijan.forcedoze

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.media2.common.SessionPlayer
import androidx.media2.session.MediaController
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import androidx.media2.session.SessionCommandGroup
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class NotificationService : NotificationListenerService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = WeakReference(this)
    }

    private fun getNotifications(): List<StatusBarNotification> {
        return try {
            activeNotifications.sortedBy { it.postTime }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
    fun getPlayingPackageName(callback: (String?) -> Unit) {
        try {
            val notifications = getNotifications().filter {
                it.notification.category == Notification.CATEGORY_TRANSPORT || it.notification.category == Notification.CATEGORY_SERVICE
            }
            val notification = notifications.findLast {
                it.notification.extras[NotificationCompat.EXTRA_MEDIA_SESSION] as? MediaSession.Token != null
            }
            if (notification != null) {
                val token = notification.notification.extras[NotificationCompat.EXTRA_MEDIA_SESSION] as MediaSession.Token?
                var mediaController: MediaController? = null
                val mediaSessionCallback = object : MediaController.ControllerCallback() {
                    override fun onConnected(
                        controller: MediaController,
                        allowedCommands: SessionCommandGroup
                    ) {
                        super.onConnected(controller, allowedCommands)
                        if (controller != mediaController) return
                        if (controller.playerState == SessionPlayer.PLAYER_STATE_PLAYING) {
                            callback(notification.packageName)
                        } else {
                            callback(null)
                        }
                        try {
                            mediaController?.close()
                        } catch (_: Exception) {
                        }
                    }

                }
                mediaController = MediaController.Builder(this)
                    .setSessionCompatToken(MediaSessionCompat.Token.fromToken(token))
                    .setControllerCallback(
                        Executors.newSingleThreadExecutor(),
                        mediaSessionCallback
                    )
                    .build()
            } else {
                callback(null)
            }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    companion object {
        private var instance: WeakReference<NotificationService>? = null
        @JvmStatic fun getInstance(): NotificationService? {
            return instance?.get()
        }
    }
}