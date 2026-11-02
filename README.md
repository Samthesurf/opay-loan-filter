# Opay Loan Filter

If you use OPay in Nigeria, you know the problem. Your transaction alerts
are useful, but buried between them is a steady stream of loan promos,
EaseMoni offers, and "get funded" spam. Turning off OPay notifications
entirely is not an option, because then you miss your actual debit and
credit alerts. So I built this: a tiny app that watches your notifications
and quietly swipes away only the loan junk.

## What it does

It runs a notification listener in the background. Every time OPay posts
a notification, the app checks it against two lists: the app packages it
watches (just OPay by default) and a list of keywords (loan, easemoni,
borrow, cash advance, and friends). If both match, the notification gets
dismissed before it annoys you. Everything else, transfers, receipts,
airtime top-ups, stays exactly where it is.

You can edit both lists in the app, and there is a little test box where
you can type any text and see whether the filter would remove it or keep
it. It also keeps a count of how many promos it has blocked, which is
oddly satisfying to watch climb.

## Getting started

The easiest path is to grab the release APK, install it, and open the app:

1. Install the APK on your phone.
2. Tap "Grant notification access" and enable Opay Loan Filter in the
   system settings that open.
3. Flip the "Remove loan promos" switch on. That is it.

Keep OPay's own notifications switched on in your system settings. The
filter works by removing the promos after they arrive, so you might catch
a split-second flash of one before it disappears. That is normal.

## A few things worth knowing

- The default watch list targets the OPay app (`team.opay.pay`). If you
  ever install the standalone EaseMoni app, add its package in the app
  settings and those get filtered too.
- Matching is case-insensitive and looks at both the notification title
  and its body.
- If a promo ever slips through, open the app, check what words it used,
  and add one of them to your keywords. That is the whole maintenance
  routine.
- Everything runs on your phone. No accounts, no servers, no internet
  permission. Your notification text never leaves the device.

## Building it yourself

You need the Android SDK and Java 21. Then:

```
JAVA_HOME=/usr/lib/jvm/java-21-openjdk gradle :app:assembleRelease
```

The APK lands in `app/build/outputs/apk/release/`. There is one quirk
worth knowing if you touch the build files: the project template pins
`jvmToolchain(17)`, which tries to download a JDK through the foojay
resolver and times out in some environments. It is set to 21 here because
that is what is installed. Unit tests live under `app/src/test` and cover
the matching logic, run them with `:app:testDebugUnitTest`.

## How the pieces fit together

- `LoanFilterListener.kt` is the notification listener service. It sees
  each posted notification, checks the package and text, and cancels the
  ones that match.
- `FilterPrefs.kt` holds the matching logic (`shouldBlock`) plus the
  stored settings. The matcher is a pure function on purpose, so it can
  be unit tested without Android.
- `MainScreen.kt` is the whole UI: status, permission shortcut, keyword
  and package editors, the sample tester, and the blocked counter.

## Disclaimer

This is an independent side project. It is not affiliated with OPay or
OPay Microfinance Bank. App and brand names are mentioned only to say
which notifications get filtered.
