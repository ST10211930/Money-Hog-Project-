package vcmsa.projects.moneyhog

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewExpensesFragment
 * This fragment allows the user to:
 * - View all expenses (with filtering by date)
 * - See total amount spent per category (POE requirement)
 * - Filter expenses by date range
 * - Clear filter to reload all expenses
 * - Go back to previous screen
 */
class ViewExpensesFragment : Fragment() {

    // UI elements
    private lateinit var etDateFrom: EditText
    private lateinit var etDateTo: EditText
    private lateinit var btnFilterExpenses: Button
    private lateinit var recyclerViewExpenses: RecyclerView
    private lateinit var tvTotalPerCategoryTitle: TextView
    private lateinit var tvTotalPerCategory: TextView

    // Firestore instance
    private val db = FirebaseFirestore.getInstance()

    // Data list for RecyclerView
    private val expenseList = mutableListOf<Expense>()

    // Adapter for RecyclerView
    private lateinit var adapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_expenses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        etDateFrom = view.findViewById(R.id.etDateFrom)
        etDateTo = view.findViewById(R.id.etDateTo)
        btnFilterExpenses = view.findViewById(R.id.btnFilterExpenses)
        recyclerViewExpenses = view.findViewById(R.id.recyclerViewExpenses)
        tvTotalPerCategoryTitle = view.findViewById(R.id.tvTotalPerCategoryTitle)
        tvTotalPerCategory = view.findViewById(R.id.tvTotalPerCategory)

        // Setup RecyclerView
        recyclerViewExpenses.layoutManager = LinearLayoutManager(requireContext())
        adapter = ExpenseAdapter(expenseList)
        recyclerViewExpenses.adapter = adapter

        // Show DatePicker when user clicks date fields
        etDateFrom.setOnClickListener { showDatePicker(etDateFrom) }
        etDateTo.setOnClickListener { showDatePicker(etDateTo) }

        // Filter button → loads expenses based on selected date range
        btnFilterExpenses.setOnClickListener {
            loadExpenses()
        }

        // Clear Filter button → clears date fields and reloads all expenses
        val btnClearFilter = view.findViewById<Button>(R.id.btnClearFilter)
        btnClearFilter.setOnClickListener {
            etDateFrom.text.clear()
            etDateTo.text.clear()
            loadExpenses() // Reload all expenses
        }

        // Back button → navigates back to previous screen
        val btnBackExpenses = view.findViewById<Button>(R.id.btnBackExpenses)
        btnBackExpenses.setOnClickListener {
            findNavController().popBackStack()
        }

        // Load all expenses initially
        loadExpenses()
    }

    /**
     * Show a DatePickerDialog when user clicks on date field
     */
    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Format selected date and set to EditText
                val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                editText.setText(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Load expenses from Firestore
     * - Apply date filter if selected
     * - Display total per category (POE requirement)
     */
    private fun loadExpenses() {
        val fromDate = etDateFrom.text.toString().trim()
        val toDate = etDateTo.text.toString().trim()

        db.collection("expenses")
            .get()
            .addOnSuccessListener { result ->
                expenseList.clear()

                // Load expenses from Firestore and apply date filter
                for (document in result) {
                    val date = document.getString("date") ?: ""
                    if (isDateInRange(date, fromDate, toDate)) {
                        val expense = Expense(
                            date = date,
                            startTime = document.getString("startTime") ?: "",
                            endTime = document.getString("endTime") ?: "",
                            description = document.getString("description") ?: "",
                            category = document.getString("category") ?: "",
                            imageUrl = document.getString("imageUrl") ?: "",
                            amount = document.getDouble("amount") ?: 0.0
                        )
                        expenseList.add(expense)
                    }
                }

                // Update RecyclerView with loaded expenses
                adapter.notifyDataSetChanged()

                // Update title based on date filter
                if (fromDate.isNotEmpty() && toDate.isNotEmpty()) {
                    tvTotalPerCategoryTitle.text = "Total spent on categories during $fromDate - $toDate"
                } else {
                    tvTotalPerCategoryTitle.text = "Category Total Spent"
                }

                // Calculate total amount spent per category (POE requirement)
                displayTotalsPerCategory()
            }
    }

    /**
     * Display total amount spent per category in the TextView
     */
    private fun displayTotalsPerCategory() {
        // Create map to hold category totals
        val totalsMap = mutableMapOf<String, Double>()

        // Sum amounts per category
        for (expense in expenseList) {
            val currentTotal = totalsMap[expense.category] ?: 0.0
            totalsMap[expense.category] = currentTotal + expense.amount
        }

        // Build display string for totals
        val totalsString = StringBuilder()
        for ((category, total) in totalsMap) {
            totalsString.append("$category: R$total\n")
        }

        // Set totals string in TextView
        tvTotalPerCategory.text = totalsString.toString()
    }

    /**
     * Check if given expense date is within selected date range
     */
    private fun isDateInRange(dateStr: String, fromStr: String, toStr: String): Boolean {
        if (fromStr.isEmpty() && toStr.isEmpty()) return true
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val date = format.parse(dateStr)
            val fromDate = if (fromStr.isNotEmpty()) format.parse(fromStr) else null
            val toDate = if (toStr.isNotEmpty()) format.parse(toStr) else null

            (fromDate == null || !date!!.before(fromDate)) &&
                    (toDate == null || !date!!.after(toDate))
        } catch (e: Exception) {
            true // If parsing fails, consider the date valid
        }
    }
}
