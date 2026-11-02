package com.samuel.opayloanfilter.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation3.runtime.NavKey
import com.samuel.opayloanfilter.OPAY_PACKAGE
import com.samuel.opayloanfilter.blockedCount
import com.samuel.opayloanfilter.filterEnabled
import com.samuel.opayloanfilter.lastBlocked
import com.samuel.opayloanfilter.loadKeywords
import com.samuel.opayloanfilter.loadPackages
import com.samuel.opayloanfilter.resetStats
import com.samuel.opayloanfilter.saveKeywords
import com.samuel.opayloanfilter.savePackages
import com.samuel.opayloanfilter.shouldBlock
import com.samuel.opayloanfilter.ui.theme.OpayLoanFilterTheme

private fun isListenerGranted(context: android.content.Context): Boolean =
  NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var listenerGranted by remember { mutableStateOf(isListenerGranted(context)) }
  var enabled by remember { mutableStateOf(context.filterEnabled) }
  var keywordsText by remember { mutableStateOf(context.loadKeywords().joinToString(", ")) }
  var packagesText by remember { mutableStateOf(context.loadPackages().joinToString(", ")) }
  var testTitle by remember { mutableStateOf("Get an instant loan from EaseMoni") }
  var blocked by remember { mutableStateOf(context.blockedCount()) }
  var last by remember { mutableStateOf(context.lastBlocked()) }
  var savedNote by remember { mutableStateOf("") }

  // Refresh permission + stats every time the screen resumes (user may
  // have just granted access in system settings).
  DisposableEffect(lifecycle) {
    val obs = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        listenerGranted = isListenerGranted(context)
        enabled = context.filterEnabled
        blocked = context.blockedCount()
        last = context.lastBlocked()
      }
    }
    lifecycle.addObserver(obs)
    onDispose { lifecycle.removeObserver(obs) }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(title = { Text("Opay Loan Filter") })
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Status", style = MaterialTheme.typography.titleMedium)
          Text(
            if (listenerGranted) "Notification access: granted"
            else "Notification access: NOT granted - the filter cannot run yet."
          )
          Text(if (enabled) "Filtering: ON" else "Filtering: OFF")
          Button(onClick = {
            context.startActivity(
              Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
          }) {
            Text(if (listenerGranted) "Review notification access" else "Grant notification access")
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Remove loan promos", modifier = Modifier.weight(1f))
            Switch(
              checked = enabled,
              onCheckedChange = {
                enabled = it
                context.filterEnabled = it
              }
            )
          }
        }
      }

      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("What gets removed", style = MaterialTheme.typography.titleMedium)
          Text(
            "Only notifications from the apps below whose text contains one of the keywords. " +
              "All other OPay alerts (transfers, receipts) stay untouched."
          )
          OutlinedTextField(
            value = packagesText,
            onValueChange = { packagesText = it },
            label = { Text("App packages (comma separated)") },
            placeholder = { Text(OPAY_PACKAGE) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
          )
          OutlinedTextField(
            value = keywordsText,
            onValueChange = { keywordsText = it },
            label = { Text("Keywords (comma separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
              val kw = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
              val pkgs = packagesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
              if (kw.isNotEmpty() && pkgs.isNotEmpty()) {
                context.saveKeywords(kw)
                context.savePackages(pkgs)
                savedNote = "Saved ${kw.size} keywords, ${pkgs.size} app(s)."
              } else {
                savedNote = "Keep at least one keyword and one package."
              }
            }) { Text("Save") }
            OutlinedButton(onClick = {
              keywordsText = context.loadKeywords().joinToString(", ")
              packagesText = context.loadPackages().joinToString(", ")
              savedNote = ""
            }) { Text("Reset edits") }
          }
          if (savedNote.isNotEmpty()) Text(savedNote)
        }
      }

      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Try a sample", style = MaterialTheme.typography.titleMedium)
          OutlinedTextField(
            value = testTitle,
            onValueChange = { testTitle = it },
            label = { Text("Fake notification text") },
            modifier = Modifier.fillMaxWidth(),
          )
          val pkgs = packagesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
          val kws = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
          val wouldBlock = if (pkgs.isNotEmpty() && kws.isNotEmpty()) {
            shouldBlock(OPAY_PACKAGE, testTitle, null, pkgs, kws)
          } else false
          Text(
            if (wouldBlock) "This WOULD be removed (matches $OPAY_PACKAGE + keyword)."
            else "This would be KEPT."
          )
        }
      }

      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Blocked so far: $blocked", style = MaterialTheme.typography.titleMedium)
          Text("Last removed: ${last ?: "nothing yet"}")
          OutlinedButton(onClick = {
            context.resetStats()
            blocked = 0
            last = null
          }) { Text("Reset counter") }
        }
      }
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
  OpayLoanFilterTheme { MainScreen(onItemClick = {}) }
}
