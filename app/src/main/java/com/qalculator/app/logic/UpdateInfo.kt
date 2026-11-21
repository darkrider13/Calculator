package com.qalculator.app.logic

/*
 * The "update" button in the history page: reads the latest GitHub release.
 * The workflow names the release tag v2.0.N and the APK Qalculator-v2.0.N.apk, N = the build number.
 * (Pure logic; the network and the download are in Updater.kt.)
 */
class UpdateInfo(val tag: String, val build: Int, val apkUrl: String, val apkName: String)

/** Reads the JSON of GitHub's "latest release" call. null if it has no tag or no .apk file. */
fun parseRelease(text: String): UpdateInfo? {
    val root = try { Json.parse(text) as? Map<*, *> } catch (e: Exception) { null } ?: return null
    val tag = root["tag_name"] as? String ?: return null
    val build = Regex("([0-9]+)$").find(tag)?.value?.toIntOrNull() ?: return null
    val assets = root["assets"] as? List<*> ?: return null
    for (a in assets) {
        val m = a as? Map<*, *> ?: continue
        val name = m["name"] as? String ?: continue
        val url = m["browser_download_url"] as? String ?: continue
        if (name.endsWith(".apk")) return UpdateInfo(tag, build, url, name)
    }
    return null
}

/** The installed build number: versionCode is 100 + the GitHub run number (see app/build.gradle.kts). */
fun buildOfVersionCode(versionCode: Long): Int = (versionCode - 100L).toInt()
