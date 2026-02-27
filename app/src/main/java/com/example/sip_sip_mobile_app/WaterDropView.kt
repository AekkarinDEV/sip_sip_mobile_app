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

    // สีตัวเลขเมื่ออยู่บนพื้นหลังขาว (เหนือน้ำ)
    private val textPaintDry = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5FACE0") // primaryBlue
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // สีตัวเลขเมื่ออยู่ในน้ำ
    private val textPaintWet = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

        val textSize = radius * 0.5f
        textPaintDry.textSize = textSize
        textPaintWet.textSize = textSize
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. วาดพื้นหลังวงกลมสีขาว
        canvas.drawPath(circlePath, circlePaint)

        val textY = viewHeight / 2f - ((textPaintDry.descent() + textPaintDry.ascent()) / 2f)
        val textStr = "$percentage%"

        // 2. วาดตัวเลขสีฟ้า (จะเห็นได้ชัดบนพื้นหลังขาว)
        canvas.drawText(textStr, viewWidth / 2f, textY, textPaintDry)

        // 3. เตรียม Wave Path
        val wavePath = getWavePath()

        // 4. วาดส่วนที่เป็นน้ำและตัวเลขสีขาว (เฉพาะในส่วนที่น้ำท่วมถึง)
        canvas.save()
        // Clip ให้วาดเฉพาะในวงกลม และ เฉพาะในบริเวณคลื่นน้ำ
        canvas.clipPath(circlePath)
        canvas.clipPath(wavePath)
        
        // วาดน้ำ
        canvas.drawPath(wavePath, waterPaint)
        
        // วาดตัวเลขสีขาวทับ (จะเห็นเฉพาะส่วนที่อยู่ในน้ำ)
        canvas.drawText(textStr, viewWidth / 2f, textY, textPaintWet)
        
        canvas.restore()
    }

    private fun getWavePath(): Path {
        val waterLevelY = (viewHeight / 2f + radius) - (2 * radius * percentage / 100f)
        val waveHeight = radius * 0.08f // ปรับความสูงคลื่นให้พอดี

        val path = Path()
        path.moveTo(-viewWidth.toFloat(), viewHeight * 2f) // เริ่มจากด้านล่าง
        path.lineTo(-viewWidth.toFloat(), waterLevelY)

        val waveLength = viewWidth.toFloat()
        var x = -viewWidth.toFloat()
        while (x <= viewWidth * 2f) {
            val y = waterLevelY + waveHeight * sin(x / waveLength * 2 * Math.PI.toFloat() + waveOffset)
            path.lineTo(x, y)
            x += 10
        }

        path.lineTo(viewWidth * 2f, waterLevelY)
        path.lineTo(viewWidth * 2f, viewHeight * 2f)
        path.close()
        return path
    }

    fun setProgress(value: Int) {
        this.percentage = value.coerceIn(0, 100)
        invalidate()
    }

    fun setPercentageColor(@ColorInt color: Int) {
        textPaintDry.color = color
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
