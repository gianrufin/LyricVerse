package com.example.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.LyricVerseApplication

class LyricNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        (application as? LyricVerseApplication)?.appContainer?.mediaSessionTracker?.startObserving()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        (application as? LyricVerseApplication)?.appContainer?.mediaSessionTracker?.stopObserving()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Not used to scrape notifications; MediaSessionManager handles track telemetry cleanly.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }
}
