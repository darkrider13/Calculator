package com.qalculator.app

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.qalculator.app.ui.FLOAT_HEADER_DP
import com.qalculator.app.ui.FLOAT_TALL_DP
import com.qalculator.app.ui.FLOAT_W_DP
import com.qalculator.app.ui.FloatCalc

/** the button under the display calls this: asks for the permission once, then shows the floating calculator */
object FloatLauncher {
    fun open(ctx: Context) {
        if (!Settings.canDrawOverlays(ctx)) {
            Toast.makeText(ctx, "Turn on \"Display over other apps\" and tap again", Toast.LENGTH_LONG).show()
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                ctx.startActivity(i)
            } catch (x: Exception) {
                // no settings page on this phone
            }
            return
        }
        ContextCompat.startForegroundService(ctx, Intent(ctx, FloatService::class.java))
        // the app goes back, so the other app is visible under the floating calculator
        var c: Context = ctx
        while (c is ContextWrapper) {
            if (c is Activity) {
                c.moveTaskToBack(true)
                break
            }
            c = c.baseContext
        }
    }
}

/** Lifecycle + saved state for the ComposeView, because there is no activity in a service */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun start() {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

/** the root of the floating window: a drag that starts in the header (and passes the touch slop) moves the window */
private class DragFrame(
    ctx: Context,
    private val wm: WindowManager,
    private val lp: WindowManager.LayoutParams,
    private val headerPx: Int
) : FrameLayout(ctx) {
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var inHeader = false
    private var dragging = false

    private fun down(e: MotionEvent) {
        downX = e.rawX
        downY = e.rawY
        startX = lp.x
        startY = lp.y
        inHeader = e.y < headerPx
        dragging = false
    }

    private fun moved(e: MotionEvent): Boolean =
        Math.abs(e.rawX - downX) > slop || Math.abs(e.rawY - downY) > slop

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) down(e)
        else if (e.actionMasked == MotionEvent.ACTION_MOVE && inHeader && moved(e)) dragging = true
        return dragging
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> down(e)
            MotionEvent.ACTION_MOVE -> {
                if (inHeader && moved(e)) dragging = true
                if (dragging) {
                    lp.x = startX + (e.rawX - downX).toInt()
                    lp.y = startY + (e.rawY - downY).toInt()
                    try {
                        wm.updateViewLayout(this, lp)
                    } catch (x: Exception) {
                        // the window is already gone
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }
}

/** keeps the floating calculator alive while another app is open (a foreground service with a notification) */
class FloatService : Service() {
    private var wm: WindowManager? = null
    private var root: DragFrame? = null
    private var params: WindowManager.LayoutParams? = null
    private val owner = OverlayOwner()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startFg()
        if (root == null) {
            if (!Settings.canDrawOverlays(this)) {
                stopSelf()
                return START_NOT_STICKY
            }
            show()
        }
        return START_NOT_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Floating calculator", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, FloatService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Calculator is running")
            .setContentText("Floating calculator is over other apps")
            .setContentIntent(open)
            .addAction(0, "Close", stop)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun show() {
        val dens = resources.displayMetrics.density
        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            (FLOAT_W_DP * dens).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = (24 * dens).toInt()
        lp.y = (140 * dens).toInt()

        val frame = DragFrame(this, manager, lp, (FLOAT_HEADER_DP * dens).toInt())
        val ctl = AppState.get(this).calcCtl
        val cv = ComposeView(this)
        cv.setContent {
            FloatCalc(
                ctl = ctl,
                onOpenApp = {
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    stopSelf()
                },
                onClose = { stopSelf() },
                onAnimating = { tall, h -> setTall(tall, h) }
            )
        }
        frame.addView(cv)
        frame.setViewTreeLifecycleOwner(owner)
        frame.setViewTreeSavedStateRegistryOwner(owner)
        owner.start()
        manager.addView(frame, lp)
        wm = manager
        root = frame
        params = lp
    }

    /** true: a fixed tall window (while the keys animate); false: the window gets exactly the content height (no jump) */
    private fun setTall(tall: Boolean, contentPx: Int) {
        val r = root ?: return
        val lp = params ?: return
        val h = if (tall) (FLOAT_TALL_DP * resources.displayMetrics.density).toInt()
        else if (contentPx > 0) contentPx
        else WindowManager.LayoutParams.WRAP_CONTENT
        if (lp.height == h) return
        lp.height = h
        try {
            wm?.updateViewLayout(r, lp)
        } catch (x: Exception) {
            // the window is already gone
        }
    }

    override fun onDestroy() {
        val r = root
        if (r != null) {
            try {
                wm?.removeView(r)
            } catch (x: Exception) {
                // already removed
            }
            root = null
        }
        owner.stop()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "float_calc"
        private const val NOTIF_ID = 17
        private const val ACTION_STOP = "com.qalculator.app.STOP_FLOAT"
    }
}
