package com.example.sip_sip_mobile_app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

enum class StatPeriod { WEEK, MONTH, YEAR }

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

    private var currentPeriod = StatPeriod.WEEK
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        barChart = findViewById(R.id.barChart)
        tvPeriod = findViewById(R.id.tvPeriod)
        tvTotal = findViewById(R.id.tvTotalValue)
        tvAverage = findViewById(R.id.tvAverageValue)
        tvBest = findViewById(R.id.tvBestValue)
        tvOverallAverage = findViewById(R.id.tvOverallAverage)
        tvOverallTotal = findViewById(R.id.tvOverallTotal)

        setupTabs()
        setupNavigation()
        updateUI()

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        Log.d("USER_CHECK", "User UID = ${auth.currentUser?.uid}")

        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        BottomNavManager(this, bottomNavView).setupBottomNavigation()
    }

    // -------------------------
    // UI SETUP
    // -------------------------

    private fun setupTabs() {
        findViewById<Button>(R.id.btnWeek).setOnClickListener {
            currentPeriod = StatPeriod.WEEK
            calendar.time = Date()
            updateUI()
        }
        findViewById<Button>(R.id.btnMonth).setOnClickListener {
            currentPeriod = StatPeriod.MONTH
            calendar.time = Date()
            updateUI()
        }
        findViewById<Button>(R.id.btnYear).setOnClickListener {
            currentPeriod = StatPeriod.YEAR
            calendar.time = Date()
            updateUI()
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

    // -------------------------
    // PERIOD DATA (ไม่ใช้ where)
    // -------------------------

    private fun loadPeriodData() {

        val user = auth.currentUser ?: return

        val (start, end, labels) = when (currentPeriod) {
            StatPeriod.WEEK -> getWeekRange()
            StatPeriod.MONTH -> getMonthRange()
            StatPeriod.YEAR -> getYearRange()
        }

        val entries = MutableList(labels.size) { 0f }

        var total = 0f
        var best = 0f
        var count = 0
        var loaded = 0

        val tempCal = start.clone() as Calendar
        var index = 0

        while (!tempCal.after(end)) {

            val currentIndex = index  // 🔥 สำคัญมาก
            val dateString = dateFormat.format(tempCal.time)
            val docId = "${user.uid}_$dateString"

            db.collection("consumptions")
                .document(docId)
                .get()
                .addOnSuccessListener { document ->

                    val intake = document.getLong("total_intake_ml")?.toFloat() ?: 0f

                    if (currentIndex < entries.size) {
                        entries[currentIndex] = intake / 1000f
                    }

                    if (intake > 0) {
                        total += intake
                        if (intake > best) best = intake
                        count++
                    }

                    loaded++

                    if (loaded == labels.size) {

                        val barEntries = entries.mapIndexed { i, value ->
                            BarEntry(i.toFloat(), value)
                        }

                        setupChart(barEntries, labels)
                        updateSummary(total, best, count)
                    }
                }

            tempCal.add(Calendar.DATE, 1)
            index++
        }
    }


    // -------------------------
    // CHART
    // -------------------------

    private fun setupChart(entries: List<BarEntry>, labels: List<String>) {

        val dataSet = BarDataSet(entries, "")
        dataSet.color = "#5DADE2".toColorInt()
        dataSet.valueTextSize = 10f

        barChart.data = BarData(dataSet)

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setNoDataText("")

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
        }

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        barChart.animateY(800)
        barChart.invalidate()
    }

    private fun updateSummary(total: Float, best: Float, count: Int) {
        tvTotal.text = String.format("%.2f L", total / 1000f)
        tvAverage.text =
            if (count > 0)
                String.format("%.2f L", (total / count) / 1000f)
            else "0.00 L"
        tvBest.text = String.format("%.2f L", best / 1000f)
    }

    // -------------------------
    // OVERALL SUMMARY
    // -------------------------

    private fun loadOverallSummary() {

        val user = auth.currentUser ?: return

        db.collection("consumptions")
            .orderBy(com.google.firebase.firestore.FieldPath.documentId())
            .startAt(user.uid + "_")
            .endAt(user.uid + "_\uf8ff")
            .get()
            .addOnSuccessListener { documents ->

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

                tvOverallAverage.text =
                    String.format("%.2f L", avg / 1000f)

                tvOverallTotal.text =
                    String.format("%.2f L", totalAll / 1000f)
            }
            .addOnFailureListener {
                Log.e("Statistics", "Error loading overall summary", it)
            }
    }




    // -------------------------
    // DATE RANGES
    // -------------------------

    private fun getWeekRange(): Triple<Calendar, Calendar, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val end = start.clone() as Calendar
        end.add(Calendar.DATE, 6)
        return Triple(start, end, listOf("อา", "จ", "อ", "พ", "พฤ", "ศ", "ส"))
    }

    private fun getMonthRange(): Triple<Calendar, Calendar, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        val end = start.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        val labels = (1..start.getActualMaximum(Calendar.DAY_OF_MONTH)).map { it.toString() }
        return Triple(start, end, labels)
    }

    private fun getYearRange(): Triple<Calendar, Calendar, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_YEAR, 1)
        val end = start.clone() as Calendar
        end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR))
        val labels = listOf(
            "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
            "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."
        )
        return Triple(start, end, labels)
    }
}
