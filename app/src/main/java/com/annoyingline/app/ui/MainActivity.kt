package com.annoyingline.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.annoyingline.app.R
import com.annoyingline.app.data.PreferenceManager
import com.annoyingline.app.service.OverlayService
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferenceManager
    private val handler = Handler(Looper.getMainLooper())
    private var isHoldingToDisable = false
    private var holdProgress = 0
    private val holdTarget = 600 // 600 steps of 100ms = 60 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferenceManager(this)

        setupPermissionsSection()
        setupOverlaySettings()
        setupAdvancedSettings()
        checkSamsungWarning()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
        findViewById<SwitchMaterial>(R.id.switchOverlay).isChecked = prefs.isOverlayEnabled
        
        // Ensure service state matches preferences
        if (prefs.isOverlayEnabled && Settings.canDrawOverlays(this)) {
            startOverlayService()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun setupPermissionsSection() {
        val btnGrant = findViewById<Button>(R.id.btnGrantPermission)
        btnGrant.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }

    private fun updatePermissionUI() {
        val btnGrant = findViewById<Button>(R.id.btnGrantPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            btnGrant.text = "Permission Granted ✓"
            btnGrant.isEnabled = false
            btnGrant.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        } else {
            btnGrant.text = getString(R.string.grant_permission)
            btnGrant.isEnabled = true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlaySettings() {
        val switchOverlay = findViewById<SwitchMaterial>(R.id.switchOverlay)
        val progressDisable = findViewById<ProgressBar>(R.id.progressDisable)
        val tvHoldToDisable = findViewById<TextView>(R.id.tvHoldToDisable)
        val switchBoot = findViewById<SwitchMaterial>(R.id.switchBoot)
        val seekPosition = findViewById<SeekBar>(R.id.seekPosition)

        switchBoot.isChecked = prefs.isBootStartEnabled
        switchBoot.setOnCheckedChangeListener { _, isChecked ->
            prefs.isBootStartEnabled = isChecked
        }

        seekPosition.progress = (prefs.linePosition * 100).toInt()
        seekPosition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val floatPos = (progress / 100f).coerceIn(0.1f, 0.9f)
                    prefs.linePosition = floatPos
                    // Service picks up drift internally, but we can restart to apply instantly
                    if (prefs.isOverlayEnabled) startOverlayService()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val disableRunnable = object : Runnable {
            override fun run() {
                if (!isHoldingToDisable) return
                holdProgress++
                val progressPercentage = ((holdProgress.toFloat() / holdTarget.toFloat()) * 100).toInt()
                    progressDisable.progress = progressPercentage
                    if (holdProgress >= holdTarget) {
                    // Success disable
                    isHoldingToDisable = false
                    progressDisable.visibility = View.GONE
                    tvHoldToDisable.visibility = View.GONE
                    switchOverlay.isChecked = false
                    prefs.isOverlayEnabled = false
                    stopOverlayService()
                } else {
                    handler.postDelayed(this, 100)
                }
            }
        }

        switchOverlay.setOnTouchListener { v, event ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                // Ignore touches if no permission
                return@setOnTouchListener false
            }

            if (switchOverlay.isChecked) {
                // Trying to disable -> require hold
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isHoldingToDisable = true
                        holdProgress = 0
                        progressDisable.progress = 0
                        progressDisable.visibility = View.VISIBLE
                        tvHoldToDisable.visibility = View.VISIBLE
                        handler.post(disableRunnable)
                        return@setOnTouchListener true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isHoldingToDisable = false
                        handler.removeCallbacks(disableRunnable)
                        progressDisable.visibility = View.GONE
                        tvHoldToDisable.visibility = View.GONE
                        holdProgress = 0
                        return@setOnTouchListener true
                    }
                }
            } else {
                // Trying to enable -> normal click
                if (event.action == MotionEvent.ACTION_UP) {
                    switchOverlay.isChecked = true
                    prefs.isOverlayEnabled = true
                    startOverlayService()
                }
            }
            false
        }
    }

    private fun setupAdvancedSettings() {
        val btnHideIcon = findViewById<Button>(R.id.btnHideIcon)
        val btnBatteryOpt = findViewById<Button>(R.id.btnBatteryOpt)

        btnHideIcon.setOnClickListener {
            showHideIconConfirmation()
        }

        btnBatteryOpt.setOnClickListener {
            requestBatteryOptimization()
        }
    }

    private fun checkSamsungWarning() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val tvTitle = findViewById<TextView>(R.id.tvSamsungWarningTitle)
        val tvDesc = findViewById<TextView>(R.id.tvSamsungWarningDesc)
        
        if (manufacturer.contains("samsung")) {
            tvTitle.visibility = View.VISIBLE
            tvDesc.visibility = View.VISIBLE
            tvTitle.text = getString(R.string.samsung_warning)
            tvDesc.text = getString(R.string.samsung_warning_msg)
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun requestBatteryOptimization() {
        @SuppressLint("BatteryLife")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback if device doesn't support the direct intent
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                try {
                    startActivity(fallbackIntent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun showHideIconConfirmation() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.hide_launcher_icon)
            .setMessage("Are you sure? Once hidden, you can only launch or disable the app by going to Android Settings -> Apps -> AnnoyingLine.")
            .setPositiveButton("Hide It") { _, _ ->
                hideLauncherIcon()
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // 5 second delay before OK button is enabled
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        handler.postDelayed({
            if (dialog.isShowing) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }, 5000)
    }

    private fun hideLauncherIcon() {
        val p = packageManager
        val componentName = ComponentName(this, MainActivity::class.java)
        p.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        prefs.isLauncherIconHidden = true
    }
}
