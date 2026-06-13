package com.notchdroid.ui

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.notchdroid.R
import com.notchdroid.databinding.ActivityNotchCalibrationBinding
import com.notchdroid.service.NotchOverlayService
import com.notchdroid.util.CutoutHelper
import com.notchdroid.util.Prefs

class NotchCalibrationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NotchDroid"
    }

    private lateinit var binding: ActivityNotchCalibrationBinding
    private var pillX = 0f
    private var pillY = 0f
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotchCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.calibrate_notch_title)

        val placement = CutoutHelper.getPlacement(this, Prefs.getNotchPosition(this))
        pillX = if (Prefs.useManualNotchPosition(this)) {
            Prefs.getManualNotchX(this).toFloat()
        } else {
            placement.x.toFloat()
        }
        pillY = if (Prefs.useManualNotchPosition(this)) {
            Prefs.getManualNotchY(this).toFloat()
        } else {
            placement.y.toFloat()
        }

        positionPill()

        binding.draggablePill.post { positionPill() }

        binding.draggablePill.setOnTouchListener { view, event ->
            handleDrag(view, event)
        }

        binding.btnSaveNotch.setOnClickListener {
            val x = pillX.toInt()
            val y = pillY.toInt()
            Prefs.setManualNotchOffset(this, x, y)
            Log.d(TAG, "Manual notch position saved: x=$x y=$y")
            Toast.makeText(this, R.string.notch_position_saved, Toast.LENGTH_SHORT).show()
            NotchOverlayService.reposition(this)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun handleDrag(view: View, event: MotionEvent): Boolean {
        val parent = view.parent as FrameLayout
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchOffsetX = event.rawX - view.x
                touchOffsetY = event.rawY - view.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.rawX - touchOffsetX
                val newY = event.rawY - touchOffsetY
                pillX = newX.coerceIn(0f, (parent.width - view.width).toFloat())
                pillY = newY.coerceIn(0f, (parent.height - view.height).toFloat())
                positionPill()
                return true
            }
        }
        return false
    }

    private fun positionPill() {
        binding.draggablePill.x = pillX
        binding.draggablePill.y = pillY
    }
}
