package vcmsa.projects.moneyhog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarEntry.*
import com.github.mikephil.charting.data.BarData.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GraphFragment : Fragment() {

    private lateinit var spinnerMonthGraph: Spinner
    private lateinit var barChart: BarChart

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements
        spinnerMonthGraph = view.findViewById(R.id.spinnerMonthGraph)
        barChart = view.findViewById(R.id.barChart)
        val btnBackGraph = view.findViewById<Button>(R.id.btnBackGraph)

        // Setup Month Spinner
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonthGraph.adapter = monthAdapter

        // When user selects a month → load graph
        spinnerMonthGraph.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMonth = months[position]
                loadGraphData(selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Back button → go back
        btnBackGraph.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * Load budget goals and expenses for selected month and display graph
     */
    private fun loadGraphData(selectedMonth: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        // Step 1: Load Budget Goals
        db.collection("budget_goals")
            .whereEqualTo("userEmail", userEmail)
            .whereEqualTo("month", selectedMonth)
            .get()
            .addOnSuccessListener { budgetGoalsResult ->
                // Map budget goals by category
                val budgetGoalsMap = mutableMapOf<String, Pair<Int, Int>>() // category → (minGoal, maxGoal)
                for (doc in budgetGoalsResult) {
                    val category = doc.getString("category") ?: ""
                    val minGoal = (doc.getLong("minGoal") ?: 0L).toInt()
                    val maxGoal = (doc.getLong("maxGoal") ?: 0L).toInt()
                    budgetGoalsMap[category] = Pair(minGoal, maxGoal)
                }

                // Step 2: Load Expenses
                db.collection("expenses")
                    .get()
                    .addOnSuccessListener { expensesResult ->
                        val categoryTotals = mutableMapOf<String, Double>() // category → totalSpent

                        for (doc in expensesResult) {
                            val category = doc.getString("category") ?: ""
                            val date = doc.getString("date") ?: ""
                            val amount = doc.getDouble("amount") ?: 0.0

                            // Only include expenses that match selected month
                            if (date.contains(getMonthNumber(selectedMonth))) {
                                categoryTotals[category] = categoryTotals.getOrDefault(category, 0.0) + amount
                            }
                        }

                        // Step 3: Display Bar Chart
                        displayBarChart(budgetGoalsMap, categoryTotals)
                    }
            }
    }

    /**
     * Helper to map month name → "MM" number format used in expense date strings
     */
    private fun getMonthNumber(monthName: String): String {
        val months = mapOf(
            "January" to "-01",
            "February" to "-02",
            "March" to "-03",
            "April" to "-04",
            "May" to "-05",
            "June" to "-06",
            "July" to "-07",
            "August" to "-08",
            "September" to "-09",
            "October" to "-10",
            "November" to "-11",
            "December" to "-12"
        )
        return months[monthName] ?: ""
    }

    /**
     * Display Bar Chart for selected month
     */
    private fun displayBarChart(budgetGoalsMap: Map<String, Pair<Int, Int>>, categoryTotals: Map<String, Double>) {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        var index = 0f

        for ((category, totalSpent) in categoryTotals) {
            entries.add(BarEntry(index, totalSpent.toFloat()))
            labels.add(category)
            index += 1f
        }

        val dataSet = BarDataSet(entries, "Total Spent Per Category")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()

        val data = BarData(dataSet)
        data.barWidth = 0.9f

        barChart.data = data

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        // Clear old LimitLines
        barChart.axisLeft.removeAllLimitLines()

        // Calculate avg min/max goals → POE requirement
        val avgMinGoal = budgetGoalsMap.values.map { it.first }.average().toFloat()
        val avgMaxGoal = budgetGoalsMap.values.map { it.second }.average().toFloat()

        val minGoalLine = LimitLine(avgMinGoal, "Avg Min Goal")
        minGoalLine.lineWidth = 2f
        minGoalLine.lineColor = android.graphics.Color.GREEN
        minGoalLine.textColor = android.graphics.Color.GREEN
        minGoalLine.textSize = 12f

        val maxGoalLine = LimitLine(avgMaxGoal, "Avg Max Goal")
        maxGoalLine.lineWidth = 2f
        maxGoalLine.lineColor = android.graphics.Color.RED
        maxGoalLine.textColor = android.graphics.Color.RED
        maxGoalLine.textSize = 12f

        val yAxis = barChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.addLimitLine(minGoalLine)
        yAxis.addLimitLine(maxGoalLine)

        barChart.axisRight.isEnabled = false

        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.invalidate() // refresh
    }

}
