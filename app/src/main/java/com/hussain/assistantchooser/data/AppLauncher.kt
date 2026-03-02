package com.hussain.assistantchooser.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast

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
