package com.hussain.assistantchooser.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.hussain.assistantchooser.core.AssistantApp

private const val TAG = "AppLauncher"

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

        if (pkg == "com.openai.chatgpt") {
            runCatching {
                context.startActivity(Intent().apply {
                    component = ComponentName(pkg, "com.openai.voice.assistant.AssistantActivity")
                    action    = Intent.ACTION_ASSIST
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            }
        }

        val resolvers = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_ASSIST), PackageManager.MATCH_ALL
        )
        val ri = resolvers.firstOrNull { it.activityInfo.packageName == pkg }
        if (ri != null) {
            context.startActivity(Intent(Intent.ACTION_ASSIST).apply {
                component = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
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
 * Robustly launches an AssistantApp, handling direct intents, system shortcuts, 
 * and fallback assistant-style launching.
 */
fun launchAppOrShortcut(context: Context, app: AssistantApp, openDirectly: Boolean = true): Boolean {
    Log.d(TAG, "Launching: ${app.name} (${app.packageName}) intents=${app.intents?.size} shortcut=${app.shortcutId}")

    if ((!openDirectly) && app.shortcutId == null) {
        launchAssistantForPackage(context, app.packageName)
        return true
    }

    try {
        // 1. Try launching via direct manifest-parsed intents (most reliable)
        if (!app.intents.isNullOrEmpty()) {
            app.intents.forEach { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            if (app.intents.size > 1) {
                context.startActivities(app.intents.toTypedArray())
            } else {
                context.startActivity(app.intents[0])
            }
            return true
        }

        // 2. Try launching via system shortcut API
        if (app.shortcutId != null) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
            launcherApps?.startShortcut(
                app.packageName, 
                app.shortcutId, 
                null, null, 
                app.userHandle ?: android.os.Process.myUserHandle()
            )
            return true
        }

        // 3. Try standard launch intent
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }

        // 4. Final fallback: launch as assistant
        launchAssistantForPackage(context, app.packageName)
        return true

    } catch (e: SecurityException) {
        Log.e(TAG, "SecurityException launching ${app.name}", e)
        Toast.makeText(context, "Can't launch this shortcut — set Assistant Chooser as your default launcher, or open the app directly.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Log.e(TAG, "Error launching ${app.name}", e)
        Toast.makeText(context, "Launch failed: ${e.javaClass.simpleName}", Toast.LENGTH_SHORT).show()
    }

    return false
}
