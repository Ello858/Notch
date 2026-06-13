package com.notchdroid.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.notchdroid.R
import com.notchdroid.databinding.ActivityOnboardingBinding
import com.notchdroid.service.NotchOverlayService
import com.notchdroid.util.NotchPosition
import com.notchdroid.util.PermissionHelper
import com.notchdroid.util.Prefs

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var step = 0

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 100
    }

    private val calibrationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* position saved in calibration activity */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCalibrateNotch.setOnClickListener { launchCalibration() }
        binding.btnCalibrateSettings.setOnClickListener { launchCalibration() }

        if (Prefs.isOnboardingComplete(this) && PermissionHelper.allPermissionsGranted(this)) {
            showSettingsAndStart()
            return
        }

        binding.btnAction.setOnClickListener { onActionClick() }
        binding.creditsText.text = getString(R.string.credits_made_by)
        showStep(0)
    }

    override fun onResume() {
        super.onResume()
        if (step in 1..2) updatePermissionStatus()
    }

    private fun launchCalibration() {
        calibrationLauncher.launch(Intent(this, NotchCalibrationActivity::class.java))
    }

    private fun showSettingsAndStart() {
        binding.stepTitle.text = getString(R.string.about_title)
        binding.stepBody.text = "${getString(R.string.about_body)}\n\n${getString(R.string.settings_running)}"
        binding.positionPicker.visibility = View.GONE
        binding.permissionStatus.visibility = View.GONE
        binding.creditsText.text = getString(R.string.credits_made_by)
        binding.creditsText.visibility = View.VISIBLE
        binding.btnCalibrateSettings.visibility = View.VISIBLE
        binding.btnAction.text = getString(R.string.btn_done)
        binding.btnAction.setOnClickListener {
            NotchOverlayService.start(this)
            finish()
        }
        NotchOverlayService.start(this)
    }

    private fun showStep(newStep: Int) {
        step = newStep
        binding.positionPicker.visibility = View.GONE
        binding.permissionStatus.visibility = View.GONE
        binding.btnCalibrateSettings.visibility = View.GONE

        when (step) {
            0 -> {
                binding.stepTitle.text = getString(R.string.onboarding_welcome_title)
                binding.stepBody.text = getString(R.string.onboarding_welcome_body)
                binding.btnAction.text = getString(R.string.btn_continue)
            }
            1 -> {
                binding.stepTitle.text = getString(R.string.onboarding_overlay_title)
                binding.stepBody.text = getString(R.string.onboarding_overlay_body)
                binding.btnAction.text = getString(R.string.btn_grant_permission)
                binding.permissionStatus.visibility = View.VISIBLE
                updatePermissionStatus()
            }
            2 -> {
                binding.stepTitle.text = getString(R.string.onboarding_notification_title)
                binding.stepBody.text = getString(R.string.onboarding_notification_body)
                binding.btnAction.text = getString(R.string.btn_grant_permission)
                binding.permissionStatus.visibility = View.VISIBLE
                updatePermissionStatus()
            }
            3 -> {
                binding.stepTitle.text = getString(R.string.onboarding_position_title)
                binding.stepBody.text = getString(R.string.onboarding_position_body)
                binding.positionPicker.visibility = View.VISIBLE
                binding.btnAction.text = getString(R.string.btn_done)
                setupPositionPicker()
            }
        }
    }

    private fun setupPositionPicker() {
        when (Prefs.getNotchPosition(this)) {
            NotchPosition.TOP_CENTER -> binding.posTopCenter.isChecked = true
            NotchPosition.TOP_LEFT -> binding.posTopLeft.isChecked = true
            NotchPosition.TOP_RIGHT -> binding.posTopRight.isChecked = true
        }
    }

    private fun savePosition() {
        val position = when (binding.positionGroup.checkedRadioButtonId) {
            R.id.posTopLeft -> NotchPosition.TOP_LEFT
            R.id.posTopRight -> NotchPosition.TOP_RIGHT
            else -> NotchPosition.TOP_CENTER
        }
        Prefs.setNotchPosition(this, position)
    }

    private fun updatePermissionStatus() {
        val granted = when (step) {
            1 -> PermissionHelper.canDrawOverlays(this)
            2 -> PermissionHelper.hasNotificationAccess(this)
            else -> false
        }
        binding.permissionStatus.text = if (granted) {
            getString(R.string.permission_granted)
        } else {
            getString(R.string.permission_needed)
        }
        binding.permissionStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (granted) R.color.accent else R.color.gray_text
            )
        )
    }

    private fun onActionClick() {
        when (step) {
            0 -> showStep(1)
            1 -> {
                if (PermissionHelper.canDrawOverlays(this)) {
                    showStep(2)
                } else {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
            2 -> {
                if (PermissionHelper.hasNotificationAccess(this)) {
                    requestPostNotificationsIfNeeded { showStep(3) }
                } else {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
            3 -> {
                savePosition()
                requestPostNotificationsIfNeeded { finishOnboarding() }
            }
        }
    }

    private fun requestPostNotificationsIfNeeded(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
                pendingAction = onGranted
                return
            }
        }
        onGranted()
    }

    private var pendingAction: (() -> Unit)? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            pendingAction?.invoke()
            pendingAction = null
        }
    }

    private fun finishOnboarding() {
        Prefs.setOnboardingComplete(this, true)
        NotchOverlayService.start(this)
        finish()
    }
}
