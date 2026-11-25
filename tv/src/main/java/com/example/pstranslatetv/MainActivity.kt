package com.example.pstranslatetv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : Activity() {

    private lateinit var status: TextView
    private val buttons = ArrayList<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(80, 60, 80, 60)
        root.gravity = Gravity.CENTER_HORIZONTAL

        status = TextView(this)
        status.textSize = 22f
        status.text = infoText()
        root.addView(status)

        root.addView(makeButton("\u0627\u0628\u062f\u0623 \u0627\u0644\u0627\u0633\u062a\u0642\u0628\u0627\u0644 \u0645\u0646 \u0627\u0644\u0647\u0627\u062a\u0641") { startReceiver() })
        root.addView(makeButton("\u0627\u062e\u062a\u0628\u0627\u0631 \u0646\u0627\u0641\u0630\u0629 \u0639\u0627\u0626\u0645\u0629 (PiP)") { testPip() })
        root.addView(makeButton("\u0625\u064a\u0642\u0627\u0641") { stopReceiver() })
        setContentView(root)
        buttons[0].requestFocus()
    }

    private fun infoText(): String {
        return "\u0639\u0646\u0648\u0627\u0646 \u0627\u0644\u062a\u0644\u0641\u0627\u0632: " + localIp() + "\n\u0627\u0643\u062a\u0628 \u0647\u0630\u0627 \u0627\u0644\u0639\u0646\u0648\u0627\u0646 \u0641\u064a \u062a\u0637\u0628\u064a\u0642 \u0627\u0644\u0647\u0627\u062a\u0641 (\u062e\u0627\u0646\u0629 \u0639\u0646\u0648\u0627\u0646 IP \u0644\u0644\u062a\u0644\u0641\u0627\u0632)\u060c \u062b\u0645 \u0627\u0636\u063a\u0637 \u0627\u0628\u062f\u0623 \u0627\u0644\u0627\u0633\u062a\u0642\u0628\u0627\u0644\u060c \u062b\u0645 Home \u0648\u0627\u0641\u062a\u062d \u0645\u0635\u062f\u0631 HDMI."
    }

    private fun localIp(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val ni = en.nextElement()
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (!a.isLoopbackAddress && a is Inet4Address) return a.hostAddress ?: "?"
                }
            }
        } catch (e: Exception) {
            return "?"
        }
        return "?"
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.setOnClickListener { action() }
        buttons.add(b)
        return b
    }

    private fun startReceiver() {
        if (!Settings.canDrawOverlays(this)) {
            val cmd = "adb shell appops set " + packageName + " SYSTEM_ALERT_WINDOW allow"
            status.text = "\u0625\u0630\u0646 \u0627\u0644\u0639\u0631\u0636 \u0641\u0648\u0642 \u0627\u0644\u062a\u0637\u0628\u064a\u0642\u0627\u062a \u063a\u064a\u0631 \u0645\u0645\u0646\u0648\u062d.\n\u062c\u0631\u0651\u0628: \u0627\u0644\u0625\u0639\u062f\u0627\u062f\u0627\u062a \u062b\u0645 \u0627\u0644\u062a\u0637\u0628\u064a\u0642\u0627\u062a \u062b\u0645 \u0635\u0644\u0627\u062d\u064a\u0627\u062a \u062e\u0627\u0635\u0629 \u062b\u0645 \u0627\u0644\u0639\u0631\u0636 \u0641\u0648\u0642 \u0627\u0644\u062a\u0637\u0628\u064a\u0642\u0627\u062a.\n\u0623\u0648 \u0646\u0641\u0651\u0630 \u0627\u0644\u0623\u0645\u0631:\n" + cmd
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
            } catch (e: Exception) {
                // \u0627\u0644\u0635\u0641\u062d\u0629 \u063a\u064a\u0631 \u0645\u0648\u062c\u0648\u062f\u0629 \u0641\u064a \u0647\u0630\u0627 \u0627\u0644\u062a\u0644\u0641\u0627\u0632
            }
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        status.text = "\u0627\u0644\u0627\u0633\u062a\u0642\u0628\u0627\u0644 \u064a\u0639\u0645\u0644 \u0639\u0644\u0649 \u0627\u0644\u0645\u0646\u0641\u0630 8765.\n" + infoText()
    }

    private fun stopReceiver() {
        stopService(Intent(this, OverlayService::class.java))
        status.text = "\u062a\u0645 \u0627\u0644\u0625\u064a\u0642\u0627\u0641.\n" + infoText()
    }

    private fun testPip() {
        try {
            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            val ok = enterPictureInPictureMode(params)
            status.text = "\u0646\u062a\u064a\u062c\u0629 PiP: " + ok
        } catch (e: Exception) {
            status.text = "PiP \u063a\u064a\u0631 \u0645\u062f\u0639\u0648\u0645: " + e.message
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        for (b in buttons) b.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        status.textSize = if (isInPictureInPictureMode) 14f else 22f
        if (isInPictureInPictureMode) status.text = "\u0646\u0635 \u062a\u062c\u0631\u064a\u0628\u064a \u0644\u0644\u062a\u0631\u062c\u0645\u0629"
    }
}

