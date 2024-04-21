package eu.fyp.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.TimeZone


class Stats2 : AppCompatActivity() {

    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats2)

        // Initialize Firebase Realtime Database reference
        db = FirebaseDatabase.getInstance().reference

        // Retrieve the current user's ID from Firebase Authentication
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            // User is authenticated, proceed with accessing user ID
            // Retrieve weekly data from the database
            retrieveWeeklyData(uid)
        }
    }

    fun redirectToStatsActivity(view: View) {
        val intent = Intent(this, Stats::class.java)
        startActivity(intent)
    }

    fun redirectToHome(view: View) {
        // Start the HomeActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun retrieveWeeklyData(userId: String) {
        // Define the reference to the weekly data in the database
        val weeklyDataRef = db.child("users").child(userId).child("weeklyIntake")

        // Listen for changes in the weekly data
        weeklyDataRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Process the retrieved data
                if (dataSnapshot.exists()) {
                    // Data exists, process it
                    val weeklyDataList = mutableListOf<WeeklyIntake>()
                    for (weeklySnapshot in dataSnapshot.children) {
                        val weeklyData = weeklySnapshot.getValue(WeeklyIntake::class.java)
                        weeklyData?.let {
                            weeklyDataList.add(it)
                            Log.d("WeeklyIntake", "Carbs: ${it.carbohydrates}")
                        }
                    }
                    // Pass the list of weekly data to the method for updating the UI
                    updateCharts(weeklyDataList)
                } else {
                    // No data available
                    Toast.makeText(this@Stats2, "No weekly data available", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Error occurred while retrieving data
                Toast.makeText(this@Stats2, "Error retrieving weekly data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCharts(weeklyIntake: List<WeeklyIntake>) {
        // Update the pie chart
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        setupPieChart(pieChart, weeklyIntake)

        // Update the bar chart
        val barChart = findViewById<BarChart>(R.id.barChart)
        setupBarChart(barChart, weeklyIntake)

        // Update the line chart
        val lineChart = findViewById<LineChart>(R.id.lineChart)
        setupLineChart(lineChart, weeklyIntake)
    }

    private fun setupPieChart(pieChart: PieChart, weeklyDataList: List<WeeklyIntake>) {
        // Initialize total values of carbs, protein, and fats
        var totalCarbs = 0.0
        var totalProtein = 0.0
        var totalFats = 0.0

        // Calculate total values by iterating through the list
        for (weeklyData in weeklyDataList) {
            totalCarbs += weeklyData.carbohydrates * weeklyData.portion
            totalProtein += weeklyData.protein * weeklyData.portion
            totalFats += weeklyData.fat * weeklyData.portion
        }

        // Create entries for the pie chart
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(totalCarbs.toFloat(), "Carbs"))
        entries.add(PieEntry(totalProtein.toFloat(), "Protein"))
        entries.add(PieEntry(totalFats.toFloat(), "Fats"))

        // Define colors for the pie chart
        val colors = mutableListOf<Int>()
        colors.add(Color.parseColor("#FFA726")) // Orange for Carbs
        colors.add(Color.parseColor("#66BB6A")) // Green for Protein
        colors.add(Color.parseColor("#29B6F6")) // Blue for Fats

        // Create a PieDataSet from the entries
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors

        // Create a PieData object from the dataSet
        val data = PieData(dataSet)

        // Customize the appearance of the pie chart
        pieChart.apply {
            // Customization code...
            setData(data)
            description.isEnabled = false // Disable description
            legend.isEnabled = false
            data.setValueTextColor(Color.BLACK) // Set value text color
            data.setValueTextSize(14f)
            invalidate()
        }
    }

    private fun setupBarChart(barChart: BarChart, weeklyDataList: List<WeeklyIntake>) {
        // Initialize total values for each nutrient
        var totalFats = 0.0f
        var totalCarbs = 0.0f
        var totalProteins = 0.0f

        // Calculate total values by iterating through the list
        weeklyDataList.forEach { weeklyData ->
            totalFats += weeklyData.fat.toFloat() * weeklyData.portion.toFloat()
            totalCarbs += weeklyData.carbohydrates.toFloat() * weeklyData.portion.toFloat()
            totalProteins += weeklyData.protein.toFloat() * weeklyData.portion.toFloat()
        }

        // Create entries for the bar chart
        val entries = mutableListOf<BarEntry>()
        entries.add(BarEntry(0f, totalFats))
        entries.add(BarEntry(1f, totalCarbs))
        entries.add(BarEntry(2f, totalProteins))

        // Define labels for x-axis
        val labels = listOf("Fats", "Carbs", "Protein")

        // Define colors for the bar chart
        val colors = mutableListOf<Int>()
        colors.add(Color.parseColor("#29B6F6")) // Blue for Fats
        colors.add(Color.parseColor("#FFA726")) // Orange for Carbs
        colors.add(Color.parseColor("#66BB6A")) // Green for Protein

        // Create a BarDataSet from the entries
        val dataSet = BarDataSet(entries, "")
        dataSet.colors = colors

        // Customize appearance of the bar chart
        val data = BarData(dataSet)
        barChart.apply {
            setData(data)
            description.isEnabled = false // Disable description
            axisRight.isEnabled = false // Disable right axis
            legend.isEnabled = false
            data.setValueTextColor(Color.BLACK) // Set value text color
            data.setValueTextSize(14f)
            invalidate()
        }

        // Customize x-axis
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM // Set x-axis position to bottom
            textSize = 14f // Set x-axis label text size
            valueFormatter = IndexAxisValueFormatter(labels) // Set custom labels
        }
    }

    private fun setupLineChart(lineChart: LineChart, weeklyDataList: List<WeeklyIntake>) {
        // Initialize an array to store the accumulated calorie values for each day of the week
        val calorieValues = FloatArray(7) { 0f } // Index 0 represents Sunday, index 1 represents Monday, and so on

        // Get the timezone for Malaysia/Kuala_Lumpur
        val timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

        // Iterate through the weekly data list to accumulate calorie values for each day of the week
        for (weeklyData in weeklyDataList) {
            // Convert the timestamp to a Calendar instance in the Malaysia/Kuala_Lumpur timezone
            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = weeklyData.confirmTime
            }

            // Get the day of the week from the Calendar instance (1 for Sunday, 2 for Monday, ..., 7 for Saturday)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Adjust the day of the week to start from 0 (Sunday) instead of 1
            val adjustedDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 0 else dayOfWeek - 1

            // Accumulate the calorie value for the corresponding day of the week
            calorieValues[adjustedDayOfWeek] += weeklyData.calories.toFloat() * weeklyData.portion.toFloat()
        }

        // Create entries for the line chart
        val entries = mutableListOf<Entry>()
        for (i in calorieValues.indices) {
            entries.add(Entry(i.toFloat(), calorieValues[i]))
        }

        // Create a LineDataSet from the entries
        val dataSet = LineDataSet(entries, "Calories")

        dataSet.color = Color.RED // Set line color
        dataSet.setCircleColor(Color.RED) // Set circle color
        dataSet.valueTextColor = Color.BLACK // Set value text color

        // Create a LineData object from the dataSet
        val data = LineData(dataSet)

        // Customize the appearance of the line chart
        lineChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"))
            description.isEnabled = false // Disable description
            axisRight.isEnabled = false // Disable right axis
            legend.isEnabled = false
            data.setValueTextSize(14f)
            data.setValueTextColor(Color.BLACK) // Set value text color
            setData(data) // Set the data to the line chart
            invalidate() // Refresh the chart
        }
        lineChart.xAxis.apply {
            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM // Set x-axis position to bottom
            textSize = 14f // Set x-axis label text size
        }
    }
}


