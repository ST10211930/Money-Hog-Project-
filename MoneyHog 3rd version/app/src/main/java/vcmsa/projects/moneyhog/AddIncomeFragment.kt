package vcmsa.projects.moneyhog

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
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

class AddIncomeFragment : Fragment() {

    // UI components
    private lateinit var etIncomeDate: EditText
    private lateinit var etIncomeAmount: EditText
    private lateinit var etIncomeDescription: EditText
    private lateinit var btnSaveIncome: Button
    private lateinit var barChartIncome: BarChart

    // Firebase reference
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_income, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Link views
        etIncomeDate = view.findViewById(R.id.etIncomeDate)
        etIncomeAmount = view.findViewById(R.id.etIncomeAmount)
        etIncomeDescription = view.findViewById(R.id.etIncomeDescription)
        btnSaveIncome = view.findViewById(R.id.btnSaveIncome)
        barChartIncome = view.findViewById(R.id.barChartIncome)
        val btnBackIncome = view.findViewById<Button>(R.id.btnBackIncome)

        // Setup date picker
        etIncomeDate.setOnClickListener {
            showDatePicker(etIncomeDate)
        }

        // Save income button → calls saveIncome() + refresh chart
        btnSaveIncome.setOnClickListener {
            saveIncome()
            loadIncomeVsExpensesChart()
        }

        // Back button → return to ExpensesFragment
        btnBackIncome.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Load chart initially
        loadIncomeVsExpensesChart()
    }

    // Show DatePickerDialog
    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, year, month, dayOfMonth ->
                val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                editText.setText(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Save Income to Firestore
    private fun saveIncome() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val date = etIncomeDate.text.toString().trim()
        val amount = etIncomeAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
        val description = etIncomeDescription.text.toString().trim()

        if (date.isEmpty() || amount <= 0.0) {
            Toast.makeText(requireContext(), "Please enter valid date and amount", Toast.LENGTH_SHORT).show()
            return
        }

        val income = hashMapOf(
            "userEmail" to userEmail, // Save user email
            "date" to date,
            "amount" to amount,
            "description" to description
        )

        db.collection("income")
            .add(income)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Income saved!", Toast.LENGTH_SHORT).show()
                clearFields()
                loadIncomeVsExpensesChart() // Reload chart after saving
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error saving income: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Clear input fields
    private fun clearFields() {
        etIncomeDate.text.clear()
        etIncomeAmount.text.clear()
        etIncomeDescription.text.clear()
    }

    // Load Income vs Expenses chart → BarChart
    private fun loadIncomeVsExpensesChart() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        val expenseTotals = mutableMapOf<String, Double>()
        val incomeTotals = mutableMapOf<String, Double>()

        // Load Expenses first → Filter by current user!
        db.collection("expenses")
            .whereEqualTo("userEmail", userEmail)
            .get()
            .addOnSuccessListener { expenseResult ->
                for (document in expenseResult) {
                    val category = document.getString("category") ?: "Expenses"
                    val amount = document.getDouble("amount") ?: 0.0
                    expenseTotals[category] = expenseTotals.getOrDefault(category, 0.0) + amount
                }

                // Load Income next → Filter by current user!
                db.collection("income")
                    .whereEqualTo("userEmail", userEmail)
                    .get()
                    .addOnSuccessListener { incomeResult ->
                        for (document in incomeResult) {
                            val amount = document.getDouble("amount") ?: 0.0
                            incomeTotals["Income"] = incomeTotals.getOrDefault("Income", 0.0) + amount
                        }

                        // DRAW CHART
                        val barEntries = ArrayList<BarEntry>()
                        val barEntriesIncome = ArrayList<BarEntry>()
                        val categories = ArrayList<String>()
                        var index = 0f

                        // Expenses first
                        expenseTotals.forEach { (category, total) ->
                            categories.add(category)
                            barEntries.add(BarEntry(index, total.toFloat()))
                            barEntriesIncome.add(BarEntry(index, 0f))
                            index++
                        }

                        // Add Income LAST
                        val incomeTotal = incomeTotals["Income"] ?: 0.0
                        categories.add("Income")
                        barEntries.add(BarEntry(index, 0f))
                        barEntriesIncome.add(BarEntry(index, incomeTotal.toFloat()))

                        // Create data sets
                        val expenseDataSet = BarDataSet(barEntries, "Expenses")
                        expenseDataSet.color = resources.getColor(R.color.purple_500)

                        val incomeDataSet = BarDataSet(barEntriesIncome, "Income")
                        incomeDataSet.color = resources.getColor(R.color.teal_700)

                        val data = BarData(expenseDataSet, incomeDataSet)
                        data.barWidth = 0.4f

                        barChartIncome.data = data
                        barChartIncome.xAxis.valueFormatter = IndexAxisValueFormatter(categories)
                        barChartIncome.xAxis.granularity = 1f
                        barChartIncome.xAxis.isGranularityEnabled = true
                        barChartIncome.xAxis.setCenterAxisLabels(true)
                        barChartIncome.xAxis.setDrawGridLines(false)
                        barChartIncome.xAxis.position = XAxis.XAxisPosition.BOTTOM

                        barChartIncome.axisLeft.axisMinimum = 0f
                        barChartIncome.axisRight.isEnabled = false

                        // Group bars
                        barChartIncome.barData.groupBars(0f, 0.2f, 0.02f)

                        barChartIncome.invalidate()
                    }
            }
    }
}
