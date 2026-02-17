package com.example.sip_sip_mobile_app

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sip_sip_mobile_app.databinding.ActivityStatisticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class Statistics : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ตั้งค่ากราฟ
        setupBarChart()

        // ปุ่มย้อนกลับ
        binding.btnBack.setOnClickListener { finish() }

        // Setup Bottom Nav
        val bottomNavManager = BottomNavManager(this, binding.layoutBottomNav.root)
        bottomNavManager.setupBottomNavigation()
    }

    private fun setupBarChart() {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 1.2f))
        entries.add(BarEntry(1f, 2.5f))
        entries.add(BarEntry(2f, 1.8f))
        entries.add(BarEntry(3f, 3.1f))
        entries.add(BarEntry(4f, 2.2f))
        entries.add(BarEntry(5f, 1.5f))
        entries.add(BarEntry(6f, 0.5f))

        val labels = arrayOf("จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.", "อา.")

        val dataSet = BarDataSet(entries, "Water")
        dataSet.color = Color.parseColor("#5DADE2")
        dataSet.valueTextSize = 10f

        binding.barChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
                granularity = 1f
            }
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
}