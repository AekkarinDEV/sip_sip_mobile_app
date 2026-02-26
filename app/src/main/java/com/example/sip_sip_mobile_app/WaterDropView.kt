package com.example.sip_sip_mobile_app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import kotlin.math.min
import kotlin.math.sin

class WaterDropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs) {

    private val circlePath = Path()
    private var waveOffset = 0f
    private var percentage = 0

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(20f, 0f, 10f, Color.parseColor("#40000000"))
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var radius = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        ValueAnimator.ofFloat(0f, 2 * Math.PI.toFloat()).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                waveOffset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h

        val padding = 20f
        radius = (min(w, h) / 2f) - padding

        circlePath.reset()
        circlePath.addCircle(w / 2f, h / 2f, radius, Path.Direction.CW)

        updateWaterColor(Color.parseColor("#81D4FA"), Color.parseColor("#29B6F6"))

        textPaint.textSize = radius * 0.6f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background circle
        canvas.drawPath(circlePath, circlePaint)

        // Clip to circle for drawing water
        canvas.save()
        canvas.clipPath(circlePath)

        // Draw water wave
        drawWave(canvas)

        // Draw percentage text over water
        val textY = viewHeight / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("$percentage%", viewWidth / 2f, textY, textPaint)

        canvas.restore()
    }

    private fun drawWave(canvas: Canvas) {
        val waterLevelY = (viewHeight / 2f + radius) - (2 * radius * percentage / 100f)
        val waveHeight = radius * 0.1f

        val wavePath = Path()
        wavePath.moveTo(0f, viewHeight.toFloat()) // Start from bottom-left
        wavePath.lineTo(0f, waterLevelY)

        val waveLength = viewWidth / 1.5f
        var x = 0f
        while (x < viewWidth) {
            val y = waterLevelY + waveHeight * sin(x / waveLength * 2 * Math.PI.toFloat() + waveOffset)
            wavePath.lineTo(x, y)
            x += 20
        }

        wavePath.lineTo(viewWidth.toFloat(), waterLevelY)
        wavePath.lineTo(viewWidth.toFloat(), viewHeight.toFloat()) // to bottom-right
        wavePath.close()

        canvas.drawPath(wavePath, waterPaint)
    }

    fun setProgress(value: Int) {
        this.percentage = value.coerceIn(0, 100)
        invalidate() // Redraw the view
    }

    fun setPercentageColor(@ColorInt color: Int) {
        textPaint.color = color
        invalidate()
    }

    fun setWaterColor(@ColorInt startColor: Int, @ColorInt endColor: Int) {
        updateWaterColor(startColor, endColor)
        invalidate()
    }

    private fun updateWaterColor(startColor: Int, endColor: Int) {
        waterPaint.shader = LinearGradient(
            viewWidth / 2f, viewHeight / 2f - radius, viewWidth / 2f, viewHeight / 2f + radius,
            startColor,
            endColor,
            Shader.TileMode.CLAMP
        )
    }
}
