package com.hussain.assistantchooser.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.hussain.assistantchooser.BuildConfig

private const val TAG = "AppLauncher"

private const val CHATGPT_PKG = "com.openai.chatgpt"

/** App Link that ChatGPT's ChatGptDeeplinkActivity opens straight into voice mode. */
private const val CHATGPT_VOICE_URL = "https://chatgpt.com/voice"

/** Intent actions that mean "start listening", in order of preference. */
private val VOICE_ACTIONS = listOf(
    "android.intent.action.VOICE_ASSIST",
    Intent.ACTION_VOICE_COMMAND,
)

/**
 * ChatGPT's voice overlay activity from older app versions. It's gone from current builds,
 * so it is only used when it still exists and is exported.
 */
private val CHATGPT_LEGACY_VOICE_ACTIVITIES = listOf(
    "com.openai.voice.assistant.AssistantActivity",
)

/**
 * Launches the most appropriate activity for a given assistant package.
 * Handles Google, ChatGPT, and generic ACTION_ASSIST / launch-intent fallbacks.
 */
fun launchAssistantForPackage(context: Context, pkg: String) {
    try {
        if (pkg == "com.google.android.googlequicksearchbox") {
            val intents = listOf(
                Intent("android.intent.action.VOICE_ASSIST").setPackage(pkg),
                Intent(Intent.ACTION_VOICE_COMMAND).setPackage(pkg),
                Intent().apply {
                    component = ComponentName(pkg, "com.google.android.voicesearch.VoiceSearchActivity")
                }
            )
            for (i in intents) {
                try {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(i)
                    return
                } catch (_: ActivityNotFoundException) { }
            }
            Toast.makeText(context, "Google Assistant not available.", Toast.LENGTH_SHORT).show()
            return
        }

        if (pkg == CHATGPT_PKG && launchChatGptVoice(context)) return

        val resolvers = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_ASSIST), PackageManager.MATCH_ALL
        )
        val ri = resolvers.firstOrNull { it.activityInfo.packageName == pkg }
        if (ri != null) {
            context.startActivity(Intent(Intent.ACTION_ASSIST).apply {
                component = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            if (BuildConfig.DEBUG) {
                Toast.makeText(context, "Opened ${ri.activityInfo.name} via ASSIST", Toast.LENGTH_LONG).show()
            }
            return
        }

        context.packageManager.getLaunchIntentForPackage(pkg)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?.let { context.startActivity(it) }
            ?: Toast.makeText(context, "Cannot launch $pkg", Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        Log.e(TAG, "launchAssistantForPackage($pkg)", e)
        context.packageManager.getLaunchIntentForPackage(pkg)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?.let { runCatching { context.startActivity(it) } }
    }
}

/**
 * Tries to open ChatGPT straight into voice mode. Returns false when no voice entry point
 * could be started, so the caller falls back to the generic ACTION_ASSIST / launcher path.
 *
 * Current ChatGPT builds (checked on 1.2026.307) route voice mode through an App Link:
 * ChatGptDeeplinkActivity handles https://chatgpt.com/voice. Their ACTION_ASSIST target
 * (AssistantProxyActivity) doesn't start voice when another app launches it, and the old
 * com.openai.voice.assistant.AssistantActivity is gone. Order:
 *  1. the https://chatgpt.com/voice deep link, pinned to the ChatGPT package,
 *  2. an exported activity that handles a voice action (VOICE_ASSIST / VOICE_COMMAND),
 *  3. the legacy voice activity, if an older ChatGPT still ships it exported.
 */
private fun launchChatGptVoice(context: Context): Boolean {
    val pm = context.packageManager

    val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse(CHATGPT_VOICE_URL))
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setPackage(CHATGPT_PKG)
    if (pm.resolveActivity(deepLink, 0) != null && tryStart(context, deepLink)) return true

    for (action in VOICE_ACTIONS) {
        val match = pm.queryIntentActivities(Intent(action).setPackage(CHATGPT_PKG), 0)
            .firstOrNull { it.activityInfo.exported }
            ?: continue
        val intent = Intent(action).setClassName(CHATGPT_PKG, match.activityInfo.name)
        if (tryStart(context, intent)) return true
    }

    for (cls in CHATGPT_LEGACY_VOICE_ACTIVITIES) {
        val info = runCatching { pm.getActivityInfo(ComponentName(CHATGPT_PKG, cls), 0) }.getOrNull()
        if (info == null || !info.exported) continue
        val intent = Intent(Intent.ACTION_ASSIST).setClassName(CHATGPT_PKG, cls)
        if (tryStart(context, intent)) return true
    }

    Log.w(TAG, "No ChatGPT voice entry point found; falling back to ACTION_ASSIST")
    return false
}

/** Starts [intent] in a new task. Returns false if the target is missing or not allowed. */
private fun tryStart(context: Context, intent: Intent): Boolean = try {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    Log.d(TAG, "Launched ${intent.component?.className} (${intent.action})")
    if (BuildConfig.DEBUG) {
        Toast.makeText(
            context,
            "Opened ${intent.component?.shortClassName ?: intent.dataString} via ${intent.action}",
            Toast.LENGTH_LONG,
        ).show()
    }
    true
} catch (e: ActivityNotFoundException) {
    Log.w(TAG, "Not found: ${intent.component}", e); false
} catch (e: SecurityException) {
    Log.w(TAG, "Not exported: ${intent.component}", e); false
}
