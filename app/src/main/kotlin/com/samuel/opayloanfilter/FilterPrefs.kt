package com.samuel.opayloanfilter

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** OPay package on the Play Store. */
const val OPAY_PACKAGE = "team.opay.pay"

/** EaseMoni loan app by OPay Microfinance Bank. */
const val EASEMONI_PACKAGE = "com.loan.cash.credit.easemoni.nigeria"

/** OKash loan app by OPay Microfinance Bank. */
const val OKASH_PACKAGE = "com.loan.cash.credit.okash.nigeria"

val DEFAULT_PACKAGES: Set<String> = setOf(OPAY_PACKAGE, EASEMONI_PACKAGE, OKASH_PACKAGE)

val DEFAULT_KEYWORDS: List<String> = listOf(
  "loan",
  "easemoni",
  "ease moni",
  "okash",
  "borrow",
  "cash advance",
  "instant cash",
  "credit limit",
  "get funded",
)

/**
 * Pure matching logic, kept free of Android APIs so it is easy to unit test.
 *
 * A notification is blocked only when it comes from a monitored package
 * AND its title/body contains at least one keyword (case-insensitive).
 * Pass every text field you extracted in [parts]; they are joined before
 * matching so a keyword in ANY field triggers the block.
 */
fun shouldBlock(
  packageName: String,
  parts: List<String?>,
  packages: Set<String> = DEFAULT_PACKAGES,
  keywords: List<String> = DEFAULT_KEYWORDS,
): Boolean {
  if (packageName !in packages) return false
  val haystack = parts.filterNotNull().joinToString(" ").lowercase()
  if (haystack.isBlank()) return false
  return keywords.any { kw ->
    val k = kw.trim().lowercase()
    k.isNotEmpty() && haystack.contains(k)
  }
}

/** Backwards-compatible overload for title + body callers (UI preview, old tests). */
fun shouldBlock(
  packageName: String,
  title: String?,
  text: String?,
  packages: Set<String> = DEFAULT_PACKAGES,
  keywords: List<String> = DEFAULT_KEYWORDS,
): Boolean = shouldBlock(packageName, listOf(title, text), packages, keywords)

private const val PREFS = "loan_filter_prefs"
private const val KEY_ENABLED = "filter_enabled"
private const val KEY_KEYWORDS = "keywords_csv"
private const val KEY_PACKAGES = "packages_csv"
private const val KEY_BLOCKED_COUNT = "blocked_count"
private const val KEY_LAST_BLOCKED = "last_blocked"

private fun prefs(context: Context): SharedPreferences =
  context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

var Context.filterEnabled: Boolean
  get() = prefs(this).getBoolean(KEY_ENABLED, true)
  set(value) { prefs(this).edit { putBoolean(KEY_ENABLED, value) } }

fun Context.loadKeywords(): List<String> {
  val csv = prefs(this).getString(KEY_KEYWORDS, null) ?: return DEFAULT_KEYWORDS
  return csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { DEFAULT_KEYWORDS }
}

fun Context.saveKeywords(keywords: List<String>) {
  prefs(this).edit { putString(KEY_KEYWORDS, keywords.joinToString(", ")) }
}

fun Context.loadPackages(): Set<String> {
  val csv = prefs(this).getString(KEY_PACKAGES, null) ?: return DEFAULT_PACKAGES
  val saved = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet().ifEmpty { DEFAULT_PACKAGES }
  // Migrate installs saved before EaseMoni/OKash were included by default:
  // if the user never customized (still exactly the old single-package default),
  // upgrade them to the new default set.
  if (saved == setOf(OPAY_PACKAGE)) return DEFAULT_PACKAGES
  return saved
}

fun Context.savePackages(packages: Set<String>) {
  prefs(this).edit { putString(KEY_PACKAGES, packages.joinToString(", ")) }
}

fun Context.blockedCount(): Int = prefs(this).getInt(KEY_BLOCKED_COUNT, 0)

fun Context.lastBlocked(): String? = prefs(this).getString(KEY_LAST_BLOCKED, null)

internal fun Context.recordBlocked(title: String?, text: String?) {
  val p = prefs(this)
  val preview = listOfNotNull(title?.takeIf { it.isNotBlank() }, text?.takeIf { it.isNotBlank() })
    .joinToString(" - ").take(180)
  p.edit {
    putInt(KEY_BLOCKED_COUNT, p.getInt(KEY_BLOCKED_COUNT, 0) + 1)
    putString(KEY_LAST_BLOCKED, preview.ifEmpty { "(empty text)" })
  }
}

fun Context.resetStats() {
  prefs(this).edit {
    putInt(KEY_BLOCKED_COUNT, 0)
    remove(KEY_LAST_BLOCKED)
  }
}
