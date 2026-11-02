package com.samuel.opayloanfilter

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Drops OPay loan/EaseMoni promo notifications, leaves everything else alone.
 *
 * Matching rule lives in [shouldBlock] so the UI preview and the service
 * always agree.
 */
class LoanFilterListener : NotificationListenerService() {

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return
    if (!filterEnabled) return
    // Never filter our own app, avoids self-inflicted loops in tests.
    if (sbn.packageName == packageName) return

    val extras = sbn.notification.extras
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
      ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

    if (shouldBlock(sbn.packageName, title, text, loadPackages(), loadKeywords())) {
      try {
        cancelNotification(sbn.key)
      } catch (_: SecurityException) {
        // Listener was disabled mid-flight; system ignores the cancel.
      }
      recordBlocked(title, text)
    }
  }
}
