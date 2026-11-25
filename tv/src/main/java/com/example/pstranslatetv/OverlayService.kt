package com.example.pstranslatetv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.net.DatagramPacket
import java.net.DatagramSocket

class OverlayService : Service() {

    private var view: TextView? = null
    private var socket: DatagramSocket? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideText() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (view == null) {
            addOverlay()
            startReceiver()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("tv", "PS Translate TV", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "tv")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("PS Translate TV")
            .build()
        startForeground(1, n)
    }

    private fun addOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val tv = TextView(this)
        tv.setTextColor(Color.WHITE)
        tv.setBackgroundColor(Color.BLACK)
        tv.textSize = 34f
        tv.setPadding(40, 24, 40, 24)
        tv.gravity = Gravity.CENTER
        tv.maxWidth = (resources.displayMetrics.widthPixels * 0.8f).toInt()
        tv.text = "\u0628\u0627\u0646\u062a\u0638\u0627\u0631 \u0627\u0644\u062a\u0631\u062c\u0645\u0629 \u0645\u0646 \u0627\u0644\u0647\u0627\u062a\u0641..."
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        lp.y = 90
        wm.addView(tv, lp)
        view = tv
    }

    private fun startReceiver() {
        Thread {
            try {
                val s = DatagramSocket(8765)
                socket = s
                val buf = ByteArray(2048)
                while (!s.isClosed) {
                    val p = DatagramPacket(buf, buf.size)
                    s.receive(p)
                    val text = String(p.data, 0, p.length, Charsets.UTF_8)
                    handler.post { showText(text) }
                }
            } catch (e: Exception) {
                // \u062a\u0645 \u0625\u063a\u0644\u0627\u0642 \u0627\u0644\u0645\u0646\u0641\u0630
            }
        }.start()
    }

    private fun showText(text: String) {
        val v = view ?: return
        handler.removeCallbacks(hideRunnable)
        if (text.isBlank()) {
            v.visibility = View.GONE
        } else {
            v.text = text.trim()
            v.visibility = View.VISIBLE
            handler.postDelayed(hideRunnable, 15000)
        }
    }

    private fun hideText() {
        val v = view
        if (v != null) v.visibility = View.GONE
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        socket?.close()
        val v = view
        if (v != null) {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            runCatching { wm.removeView(v) }
        }
        view = null
        super.onDestroy()
    }
}
