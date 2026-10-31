package com.example

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.example.notification.NotificationHelper
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AnchorViewModel

private const val TAG_APPCHECK = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

class MainActivity : ComponentActivity() {

    private val viewModel: AnchorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // Create notification channels
        NotificationHelper.createChannels(this)

        // Handle incoming Share Target or App Shortcuts Intent
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    checkClipboard()
                }

                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkClipboard()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun checkClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val item = clip.getItemAt(0)
                    val text = item.text?.toString()?.trim()
                    if (!text.isNullOrBlank() && text.length > 2) {
                        viewModel.onClipboardDetected(text)
                    }
                }
            }
        } catch (e: Exception) {
            // Clipboard access safe check
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                // Handle shared text/URL
                if ("text/plain" == intent.type) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.capture(sharedText)
                    }
                } else if (intent.type?.startsWith("image/") == true) {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    val caption = intent.getStringExtra(Intent.EXTRA_TEXT) ?: "Shared Image"
                    viewModel.capture(caption, sharedUri = imageUri?.toString())
                }
            }
            Intent.ACTION_VIEW -> {
                val actionExtra = intent.getStringExtra("action")
                if (actionExtra == "voice_capture") {
                    // Trigger voice prompt or auto-focus
                    try {
                        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your task or note...")
                        }
                        startActivity(speechIntent)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }
}

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        } else {
            try {
                FirebaseApp.getInstance()
            } catch (_: Exception) {
                null
            }
        }

        if (app == null) {
            Log.d(TAG_APPCHECK, "FirebaseApp is not initialized. App Check skipped.")
            return
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = app.persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // Use commit() synchronously so the secret is persisted before provider reads it
                prefs.edit().putString(DEBUG_SECRET_KEY, debugToken).commit()
                Log.d(TAG_APPCHECK, "App Check debug secret configured successfully.")
            }

            FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG_APPCHECK, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG_APPCHECK, "RecaptchaAppCheckProviderFactory installed.")
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG_APPCHECK, "App Check initialization bypassed or failed", e)
    }
}

