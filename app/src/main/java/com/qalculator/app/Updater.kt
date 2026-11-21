package com.qalculator.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.qalculator.app.logic.UpdateInfo
import com.qalculator.app.logic.buildOfVersionCode
import com.qalculator.app.logic.parseRelease
import java.net.HttpURLConnection
import java.net.URL

/** Checks the latest GitHub release of the app and downloads its APK. */
object Updater {
    const val REPO = "darkrider13/Qalculator-v2"

    /** the build number of the installed app (GitHub run number) */
    fun currentBuild(ctx: Context): Int {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val code: Long = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
        return buildOfVersionCode(code)
    }

    /** Asks GitHub in the background; [onDone] runs on the main thread with (release, failed). */
    fun check(onDone: (UpdateInfo?, Boolean) -> Unit) {
        Thread {
            var info: UpdateInfo? = null
            var failed = false
            try {
                val c = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
                c.connectTimeout = 10000
                c.readTimeout = 10000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                c.setRequestProperty("User-Agent", "Qalculator")
                if (c.responseCode == 200) {
                    val text = c.inputStream.bufferedReader().use { it.readText() }
                    info = parseRelease(text)
                    if (info == null) failed = true
                } else {
                    failed = true
                }
                c.disconnect()
            } catch (e: Exception) {
                failed = true
            }
            Handler(Looper.getMainLooper()).post { onDone(info, failed) }
        }.start()
    }

    /** Downloads the APK (a notification shows the progress; tap it when done to install). Falls back to the browser. */
    fun download(ctx: Context, info: UpdateInfo) {
        try {
            val req = DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle(info.apkName)
                .setDescription("Qalculator update")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, info.apkName)
            (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            Toast.makeText(ctx, "ডাউনলোড শুরু হয়েছে। শেষ হলে নোটিফিকেশনে চাপ দিয়ে ইনস্টল করুন।", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e2: Exception) {
                Toast.makeText(ctx, "ডাউনলোড শুরু করা যায়নি", Toast.LENGTH_LONG).show()
            }
        }
    }
}
