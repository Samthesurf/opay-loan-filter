package com.samuel.opayloanfilter

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Drops OPay/EaseMoni/OKash loan promo notifications, leaves everything else alone.
 *
 * Matching rule lives in [shouldBlock] so the UI preview and the service
 * always agree.
 */
class LoanFilterListener : NotificationListenerService() {

  override fun onListenerConnected() {
    super.onListenerConnected()
    // Sweep anything already showing (posted while we were dead, after
    // reboot/update, or before access was granted).
    try {
      for (sbn in activeNotifications) {
        handleNotification(sbn, fromSweep = true)
      }
    } catch (e: SecurityException) {
      Log.w(TAG, "activeNotifications sweep denied", e)
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return
    handleNotification(sbn, fromSweep = false)
  }

  private fun handleNotification(sbn: StatusBarNotification, fromSweep: Boolean) {
    if (!filterEnabled) return
    // Never filter our own app, avoids self-inflicted loops in tests.
    if (sbn.packageName == packageName) return

    val parts = extractParts(sbn)
    if (shouldBlock(sbn.packageName, parts, loadPackages(), loadKeywords())) {
      try {
        cancelNotification(sbn.key)
      } catch (e: SecurityException) {
        // Listener was disabled mid-flight; system ignores the cancel.
        Log.w(TAG, "cancel denied for ${sbn.key}", e)
        return
      }
      val title = parts.getOrNull(0)
      val body = parts.drop(1).firstOrNull { !it.isNullOrBlank() }
      recordBlocked(title, body)
      Log.i(TAG, "blocked ${sbn.packageName} (sweep=$fromSweep): $title - $body")
    }
  }

  companion object {
    private const val TAG = "LoanFilter"

    /**
     * Pull every text field OPay-style promos have been seen to use.
     * Old code read only EXTRA_TITLE + EXTRA_TEXT, so a promo carrying
     * its "Loan ..." line in bigText, title.big, subText, summary,
     * textLines, ticker or infoText slipped straight through.
     */
    internal fun extractParts(sbn: StatusBarNotification): List<String?> {
      val extras: Bundle = sbn.notification.extras
      val out = ArrayList<String?>(10)
      out += extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
      out += extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
      out += extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
      out += extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
      out += extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
      out += extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
      out += extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
      val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
      if (lines != null) {
        for (line in lines) {
          val s = line?.toString()
          if (!s.isNullOrBlank()) out += s
        }
      }
      val ticker = sbn.notification.tickerText?.toString()
      if (!ticker.isNullOrBlank()) out += ticker
      return out
    }
  }
}
