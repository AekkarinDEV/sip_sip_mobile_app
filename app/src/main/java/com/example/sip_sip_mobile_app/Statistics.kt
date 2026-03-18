package com.example.sip_sip_mobile_app

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

enum class StatPeriod {
    WEEK, MONTH, YEAR
}

class Statistics : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var barChart: BarChart
    private lateinit var tvPeriod: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvAverage: TextView
    private lateinit var tvBest: TextView
    private lateinit var tvOverallAverage: TextView
    private lateinit var tvOverallTotal: TextView

    private lateinit var btnWeek: MaterialButton
    private lateinit var btnMonth: MaterialButton
    private lateinit var btnYear: MaterialButton

    private var currentPeriod = StatPeriod.WEEK
    private val calendar = Calendar.getInstance()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.statisticsMain)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        barChart = findViewById(R.id.barChart)
        tvPeriod = findViewById(R.id.tvPeriod)
        tvTotal = findViewById(R.id.tvTotalValue)
        tvAverage = findViewById(R.id.tvAverageValue)
        tvBest = findViewById(R.id.tvBestValue)
        tvOverallAverage = findViewById(R.id.tvOverallAverage)
        tvOverallTotal = findViewById(R.id.tvOverallTotal)

        btnWeek = findViewById(R.id.btnWeek)
        btnMonth = findViewById(R.id.btnMonth)
        btnYear = findViewById(R.id.btnYear)

        setupTabs()
        setupNavigation()
        updateUI()

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        BottomNavManager(this, bottomNavView).setupBottomNavigation()
    }

    private fun setupTabs() {
        btnWeek.setOnClickListener {
            currentPeriod = StatPeriod.WEEK
            calendar.time = Date()
            updateUI()
        }
        btnMonth.setOnClickListener {
            currentPeriod = StatPeriod.MONTH
            calendar.time = Date()
            updateUI()
        }
        btnYear.setOnClickListener {
            currentPeriod = StatPeriod.YEAR
            calendar.time = Date()
            updateUI()
        }
    }

    private fun updateTabStyles() {
        val selectedColor = "#5DADE2".toColorInt()
        val unselectedColor = Color.WHITE
        val selectedTextColor = Color.WHITE
        val unselectedTextColor = "#333333".toColorInt()

        val buttons = listOf(btnWeek to StatPeriod.WEEK, btnMonth to StatPeriod.MONTH, btnYear to StatPeriod.YEAR)

        buttons.forEach { (btn, period) ->
            if (currentPeriod == period) {
                btn.backgroundTintList = ColorStateList.valueOf(selectedColor)
                btn.setTextColor(selectedTextColor)
                btn.elevation = 4f
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(unselectedColor)
                btn.setTextColor(unselectedTextColor)
                btn.elevation = 0f
            }
        }
    }

    private fun setupNavigation() {
        findViewById<ImageButton>(R.id.btnPrev).setOnClickListener {
            when (currentPeriod) {
                StatPeriod.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
                StatPeriod.MONTH -> calendar.add(Calendar.MONTH, -1)
                StatPeriod.YEAR -> calendar.add(Calendar.YEAR, -1)
            }
            updateUI()
        }

        findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            when (currentPeriod) {
                StatPeriod.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                StatPeriod.MONTH -> calendar.add(Calendar.MONTH, 1)
                StatPeriod.YEAR -> calendar.add(Calendar.YEAR, 1)
            }
            updateUI()
        }
    }

    private fun updateUI() {
        updateTabStyles()
        updatePeriodLabel()
        loadPeriodData()
        loadOverallSummary()
    }

    private fun updatePeriodLabel() {
        val locale = Locale("th", "TH")
        val sdf = when (currentPeriod) {
            StatPeriod.WEEK -> SimpleDateFormat("'สัปดาห์ที่' W yyyy", locale)
            StatPeriod.MONTH -> SimpleDateFormat("MMMM yyyy", locale)
            StatPeriod.YEAR -> SimpleDateFormat("yyyy", locale)
        }
        tvPeriod.text = sdf.format(calendar.time)
    }

    private fun loadPeriodData() {
        val user = auth.currentUser ?: return
        val (start, end, labels) = when (currentPeriod) {
            StatPeriod.WEEK -> getWeekRange()
            StatPeriod.MONTH -> getMonthRange()
            StatPeriod.YEAR -> getYearRange()
        }

        val startStr = dateFormat.format(start.time)
        val endStr = dateFormat.format(end.time)

        db.collection("consumptions")
            .whereEqualTo("user_id", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                val entries = MutableList(labels.size) { 0f }
                var total = 0f
                var best = 0f
                var count = 0

                for (doc in documents) {
                    val dateStr = doc.getString("date") ?: doc.getString("date_string") ?: ""
                    if (dateStr < startStr || dateStr > endStr) continue

                    val intake = doc.getLong("total_intake_ml")?.toFloat() ?: 0f

                    try {
                        val date = dateFormat.parse(dateStr) ?: continue
                        val cal = Calendar.getInstance()
                        cal.time = date

                        val index = when (currentPeriod) {
                            StatPeriod.WEEK -> {
                                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                                (dayOfWeek - Calendar.SUNDAY + 7) % 7
                            }
                            StatPeriod.MONTH -> {
                                cal.get(Calendar.DAY_OF_MONTH) - 1
                            }
                            StatPeriod.YEAR -> {
                                cal.get(Calendar.MONTH)
                            }
                        }

                        if (index in entries.indices) {
                            if (currentPeriod == StatPeriod.YEAR) {
                                entries[index] += intake / 1000f
                            } else {
                                entries[index] = intake / 1000f
                            }
                        }

                        if (intake > 0) {
                            total += intake
                            if (intake > best) best = intake
                            count++
                        }
                    } catch (e: Exception) {
                        Log.e("Statistics", "Error parsing date: $dateStr")
                    }
                }

                val barEntries = entries.mapIndexed { i, value -> BarEntry(i.toFloat(), value) }
                setupChart(barEntries, labels)
                updateSummary(total, best, count)
            }
            .addOnFailureListener { e ->
                Log.e("Statistics", "Error loading data: ${e.message}")
            }
    }

    private fun setupChart(entries: List<BarEntry>, labels: List<String>) {
        val dataSet = BarDataSet(entries, "")
        dataSet.color = "#5DADE2".toColorInt()
        dataSet.valueTextSize = 9f
        dataSet.setDrawValues(true) // แสดงตัวเลขบนแท่ง
        
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value > 0) String.format("%.1f", value) else ""
            }
        }

        barChart.data = BarData(dataSet)
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        
        // เปิดการเลื่อนกราฟ
        barChart.setTouchEnabled(true)
        barChart.setDragEnabled(true)
        barChart.setScaleEnabled(false) // ปิดการซูมด้วยนิ้วเพื่อให้เลื่อนง่ายขึ้น
        barChart.setPinchZoom(false)
        barChart.isDoubleTapToZoomEnabled = false

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            textSize = 10f
            textColor = Color.DKGRAY
            setLabelCount(labels.size) // พยายามโชว์ทุก Label เพราะเราเลื่อนกราฟได้แล้ว
        }
        
        barChart.axisLeft.apply {
            axisMinimum = 0f
            textColor = Color.GRAY
            textSize = 10f
            setDrawGridLines(true)
            gridColor = Color.parseColor("#EEEEEE")
        }
        
        barChart.axisRight.isEnabled = false
        barChart.extraBottomOffset = 15f 

        // จำกัดการแสดงผล (Scrollable)
        when (currentPeriod) {
            StatPeriod.MONTH -> {
                barChart.setVisibleXRangeMaximum(8f) // แสดงทีละ 8 วัน
                barChart.moveViewToX(0f) // เริ่มต้นที่วันแรก
            }
            StatPeriod.YEAR -> {
                barChart.setVisibleXRangeMaximum(6f) // แสดงทีละ 6 เดือน
                barChart.moveViewToX(0f)
            }
            StatPeriod.WEEK -> {
                barChart.fitScreen() // สัปดาห์โชว์ครบได้เลย
            }
        }

        barChart.animateY(800)
        barChart.invalidate()
    }

    private fun updateSummary(total: Float, best: Float, count: Int) {
        tvTotal.text = String.format("%.2f L", total / 1000f)
        tvAverage.text = if (count > 0) String.format("%.2f L", (total / count) / 1000f) else "0.00 L"
        tvBest.text = String.format("%.2f L", best / 1000f)
    }

    private fun loadOverallSummary() {
        val user = auth.currentUser ?: return
        db.collection("consumptions").whereEqualTo("user_id", user.uid)
            .addSnapshotListener { documents, e ->
                if (e != null || documents == null) return@addSnapshotListener
                var totalAll = 0f
                var days = 0
                for (doc in documents) {
                    val intake = doc.getLong("total_intake_ml")?.toFloat() ?: 0f
                    if (intake > 0) {
                        totalAll += intake
                        days++
                    }
                }
                val avg = if (days > 0) totalAll / days else 0f
                tvOverallAverage.text = String.format("%.2f L", avg / 1000f)
                tvOverallTotal.text = String.format("%.2f L", totalAll / 1000f)
            }
    }

    private fun getWeekRange(): Triple<Calendar, Calendar, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)

        val end = start.clone() as Calendar
        end.add(Calendar.DATE, 6)
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)

        return Triple(start, end, listOf("อา", "จ", "อ", "พ", "พฤ", "ศ", "ส"))
    }

    private fun getMonthRange(): Triple<Calendar, Calendar, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)

        val end = start.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)

        val labels = (1..start.getActualMaximum(Calendar.DAY_OF_MONTH)).map { it.toString() }
        return Triple(start, end, labels)
    }

    private fun getYearRange(): Triple<Calendar, Calendar, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_YEAR, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)

        val end = start.clone() as Calendar
        end.set(Calendar.MONTH, Calendar.DECEMBER)
        end.set(Calendar.DAY_OF_MONTH, 31)
        end.set(Calendar.HOUR_OF_DAY, 23)

        val labels = listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")
        return Triple(start, end, labels)
    }
}
