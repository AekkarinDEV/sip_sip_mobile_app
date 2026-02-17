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
import com.google.firebase.firestore.Query
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

enum class StatPeriod {
    WEEK, MONTH, YEAR
}

class StatisticsActivity : AppCompatActivity() {

    // Companion object for logging
    companion object {
        private const val TAG = "StatisticsActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var barChart: BarChart
    private lateinit var tvPeriod: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvAverage: TextView
    private lateinit var tvBest: TextView

    private var currentPeriod = StatPeriod.WEEK
    private val calendar = Calendar.getInstance()

    // Date formatter for Firestore queries, used frequently
    private val firestoreDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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

        setupTabs()
        setupNavigation()
        updateUI()

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        val bottomNavManager = BottomNavManager(this, bottomNavView)
        bottomNavManager.setupBottomNavigation()
    }

    private fun setupTabs() {
        val btnWeek = findViewById<Button>(R.id.btnWeek)
        val btnMonth = findViewById<Button>(R.id.btnMonth)
        val btnYear = findViewById<Button>(R.id.btnYear)

        btnWeek.setOnClickListener { selectPeriod(StatPeriod.WEEK) }
        btnMonth.setOnClickListener { selectPeriod(StatPeriod.MONTH) }
        btnYear.setOnClickListener { selectPeriod(StatPeriod.YEAR) }
    }

    private fun selectPeriod(period: StatPeriod) {
        currentPeriod = period
        calendar.time = Date() // Reset to today
        updateUI()
    }

    private fun setupNavigation() {
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)

        btnPrev.setOnClickListener {
            when (currentPeriod) {
                StatPeriod.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
                StatPeriod.MONTH -> calendar.add(Calendar.MONTH, -1)
                StatPeriod.YEAR -> calendar.add(Calendar.YEAR, -1)
            }
            updateUI()
        }

        btnNext.setOnClickListener {
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
        loadChartData()
    }

    private fun updatePeriodLabel() {
        val locale = Locale("th", "TH")
        val sdf = when (currentPeriod) {
            StatPeriod.WEEK -> SimpleDateFormat("'สัปดาห์ที่' W 'ของ' yyyy", locale)
            StatPeriod.MONTH -> SimpleDateFormat("MMMM yyyy", locale)
            StatPeriod.YEAR -> SimpleDateFormat("yyyy", locale)
        }
        tvPeriod.text = sdf.format(calendar.time)
    }

    private fun loadChartData() {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "No user is currently signed in.")
            return
        }

        val (range, labels) = when (currentPeriod) {
            StatPeriod.WEEK -> getWeekRange()
            StatPeriod.MONTH -> getMonthRange()
            StatPeriod.YEAR -> getYearRange()
        }
        val (start, end) = range
        val startDateString = firestoreDateFormat.format(start.time)
        val endDateString = firestoreDateFormat.format(end.time)

        Log.d(TAG, "Querying Firestore for user: ${user.uid}")
        Log.d(TAG, "Period: $currentPeriod, Start: $startDateString, End: $endDateString")

        db.collection("consumptions")
            .whereEqualTo("user_id", user.uid)
            .whereGreaterThanOrEqualTo("date_string", startDateString)
            .whereLessThanOrEqualTo("date_string", endDateString)
            .orderBy("date_string", Query.Direction.ASCENDING) // Recommended for date range queries
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Firestore query successful, found ${documents.size()} documents.")

                val entries = mutableListOf<BarEntry>()
                var totalIntake = 0f
                var bestIntake = 0f
                var activityCount = 0 // Count days for week/month, months for year

                if (documents.isEmpty) {
                    Log.d(TAG, "No consumption data found for the selected period.")
                }

                // Prepare a map of date strings to intake values from Firestore results
                val dataMap = documents.associate {
                    it.getString("date_string") to it.getLong("total_intake_ml")?.toFloat()
                }.filterValues { it != null }.mapValues { it.value!! }


                if (currentPeriod == StatPeriod.YEAR) {
                    val monthlyIntake = FloatArray(12) // 0 for Jan, 1 for Feb, etc.

                    dataMap.forEach { (dateStr, intake) ->
                        try {
                            val date = firestoreDateFormat.parse(dateStr)
                            if (date != null) {
                                val monthCal = Calendar.getInstance().apply { time = date }
                                val monthIndex = monthCal.get(Calendar.MONTH)
                                monthlyIntake[monthIndex] += intake
                            }
                        } catch (e: ParseException) {
                            Log.e(TAG, "Failed to parse date string: $dateStr", e)
                        }
                    }

                    monthlyIntake.forEachIndexed { index, intake ->
                        entries.add(BarEntry(index.toFloat(), intake))
                        if (intake > 0) {
                            totalIntake += intake
                            if (intake > bestIntake) bestIntake = intake
                            activityCount++
                        }
                    }

                } else { // Logic for WEEK and MONTH
                    val tempCal = start.clone() as Calendar
                    var index = 0f
                    while (!tempCal.after(end)) {
                        val dateKey = firestoreDateFormat.format(tempCal.time)
                        val intake = dataMap[dateKey] ?: 0f
                        entries.add(BarEntry(index, intake))

                        if (intake > 0) {
                            totalIntake += intake
                            if (intake > bestIntake) bestIntake = intake
                            activityCount++
                        }
                        index++
                        tempCal.add(Calendar.DATE, 1)
                    }
                }

                Log.d(TAG, "Chart entries created: ${entries.size}. Total intake: $totalIntake")
                setupChart(entries, labels)
                updateSummary(totalIntake, bestIntake, activityCount)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting documents from Firestore", e)
            }
    }

    private fun setupChart(entries: List<BarEntry>, labels: List<String>) {
        val dataSet = BarDataSet(entries, "ปริมาณน้ำ (ml)")
        dataSet.color = "#5DADE2".toColorInt()
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barChart.data = barData

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawValueAboveBar(true)

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = labels.size

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.isDragEnabled = true
        barChart.setScaleEnabled(true)
        barChart.setPinchZoom(true)

        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun updateSummary(total: Float, best: Float, count: Int) {
        tvTotal.text = String.format("%.2f L", total / 1000f)
        tvAverage.text = if (count > 0) String.format("%.2f L", (total / count) / 1000f) else "0.00 L"
        tvBest.text = String.format("%.2f L", best / 1000f)
    }

    private fun getWeekRange(): Pair<Pair<Calendar, Calendar>, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
        val end = start.clone() as Calendar
        end.add(Calendar.DATE, 6) // A week has 7 days. If start is Sunday, end is Saturday.
        val labels = listOf("อา", "จ", "อ", "พ", "พฤ", "ศ", "ส")
        return Pair(Pair(start, end), labels)
    }

    private fun getMonthRange(): Pair<Pair<Calendar, Calendar>, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        val end = start.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        val labels = (1..start.getActualMaximum(Calendar.DAY_OF_MONTH)).map { it.toString() }
        return Pair(Pair(start, end), labels)
    }

    private fun getYearRange(): Pair<Pair<Calendar, Calendar>, List<String>> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_YEAR, 1)
        val end = start.clone() as Calendar
        end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR))
        // Thai month labels
        val labels = listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")
        return Pair(Pair(start, end), labels)
    }
}
