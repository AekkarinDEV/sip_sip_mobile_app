package com.example.sip_sip_mobile_app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sip_sip_mobile_app.databinding.ActivityStatisticsBinding
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class Statistics : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBarChart()

        binding.btnBack.setOnClickListener { finish() }

        val bottomNavManager = BottomNavManager(this, binding.layoutBottomNav.root)
        bottomNavManager.setupBottomNavigation()
    }

    private fun setupBarChart() {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 1.20f))
        entries.add(BarEntry(1f, 2.50f))
        entries.add(BarEntry(2f, 1.80f))
        entries.add(BarEntry(3f, 3.10f))
        entries.add(BarEntry(4f, 2.20f))
        entries.add(BarEntry(5f, 1.50f))
        entries.add(BarEntry(6f, 0.50f))

        val labels = arrayOf("จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.", "อา.")

        val dataSet = BarDataSet(entries, "Water")
        // ใช้สีฟ้าสดใสตามรูป
        dataSet.color = Color.parseColor("#64B5F6")
        dataSet.valueTextColor = Color.parseColor("#333333")
        dataSet.valueTextSize = 11f

        // ฟอร์แมตตัวเลขบนแท่งกราฟให้เป็น 2 ตำแหน่ง (เช่น 1.20)
        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.2f", value)
            }
        }

        binding.barChart.apply {
            data = BarData(dataSet)

            // --- ส่วนสำคัญ: เรียกใช้ Custom Renderer เพื่อวาดขอบมน ---
            renderer = RoundedBarChartRenderer(this, animator, viewPortHandler, 20f)

            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(false) // ปิดการกดเพื่อให้กราฟดูสะอาด

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
                setDrawAxisLine(false) // ลบเส้นแกน X ออก
                granularity = 1f
                textColor = Color.parseColor("#999999")
                textSize = 12f
                yOffset = 10f // ขยับตัวอักษรลงมานิดหน่อย
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE") // เส้นตารางจางๆ
                setDrawAxisLine(false)
                textColor = Color.parseColor("#999999")
                axisMinimum = 0f
                granularity = 0.5f
            }

            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    // --- Inner Class สำหรับวาดแท่งกราฟขอบมน ---
    class RoundedBarChartRenderer(
        chart: BarChart,
        animator: ChartAnimator,
        viewPortHandler: ViewPortHandler,
        private val radius: Float
    ) : BarChartRenderer(chart, animator, viewPortHandler) {

        private val bufferRect = RectF()

        override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
            val trans = mChart.getTransformer(dataSet.axisDependency)
            mRenderPaint.color = dataSet.color
            mRenderPaint.style = Paint.Style.FILL

            val phaseX = mAnimator.phaseX
            val phaseY = mAnimator.phaseY
            val buffer = mBarBuffers[index]
            buffer.setPhases(phaseX, phaseY)
            buffer.setDataSet(index)
            buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
            buffer.setBarWidth(mChart.barData.barWidth)

            buffer.feed(dataSet)
            trans.pointValuesToPixel(buffer.buffer)

            var j = 0
            while (j < buffer.size()) {
                if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) {
                    j += 4
                    continue
                }
                if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) break

                bufferRect.set(buffer.buffer[j], buffer.buffer[j + 1], buffer.buffer[j + 2], buffer.buffer[j + 3])

                // วาดสี่เหลี่ยมขอบมน (เฉพาะมุมบน)
                c.drawRoundRect(bufferRect, radius, radius, mRenderPaint)
                j += 4
            }
        }
    }
}