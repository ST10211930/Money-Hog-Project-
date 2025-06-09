package vcmsa.projects.moneyhog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

/**
 * BudgetFragment
 * This fragment allows the user to:
 * - Set a min/max budget goal per category and month
 * - View their saved budget goals in a list
 * - Save new budget goals to Firestore
 * - Load categories dynamically from Firestore
 * - Navigate back to previous screen
 */
class BudgetFragment : Fragment() {

    // UI elements
    private lateinit var spinnerCategoryBudget: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var seekBarMinBudget: SeekBar
    private lateinit var seekBarMaxBudget: SeekBar
    private lateinit var tvMinBudget: TextView
    private lateinit var tvMaxBudget: TextView
    private lateinit var btnSaveBudget: Button
    private lateinit var recyclerViewBudgetGoals: RecyclerView

    // Firestore instance
    private val db = FirebaseFirestore.getInstance()

    // Budget goals list and adapter for RecyclerView
    private val budgetGoalsList = mutableListOf<BudgetGoal>()
    private lateinit var budgetGoalAdapter: BudgetGoalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        spinnerCategoryBudget = view.findViewById(R.id.spinnerCategoryBudget)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        seekBarMinBudget = view.findViewById(R.id.seekBarMinBudget)
        seekBarMaxBudget = view.findViewById(R.id.seekBarMaxBudget)
        tvMinBudget = view.findViewById(R.id.tvMinBudget)
        tvMaxBudget = view.findViewById(R.id.tvMaxBudget)
        btnSaveBudget = view.findViewById(R.id.btnSaveBudget)
        val btnBackBudget = view.findViewById<Button>(R.id.btnBackBudget)
        recyclerViewBudgetGoals = view.findViewById(R.id.recyclerViewBudgetGoals)

        // Setup Month Spinner
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter

        // Setup SeekBars for Min and Max Budget
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        seekBarMinBudget.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvMinBudget.text = "Min Budget: ${currencyFormat.format(progress)}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarMaxBudget.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvMaxBudget.text = "Max Budget: ${currencyFormat.format(progress)}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup RecyclerView for displaying saved budget goals
        recyclerViewBudgetGoals.layoutManager = LinearLayoutManager(requireContext())
        budgetGoalAdapter = BudgetGoalAdapter(budgetGoalsList)
        recyclerViewBudgetGoals.adapter = budgetGoalAdapter

        // Load categories and saved budget goals
        loadCategories()
        loadBudgetGoals()

        // Save button → save budget goal and refresh list
        btnSaveBudget.setOnClickListener {
            saveBudgetGoal()
        }

        // Back button → navigate back
        btnBackBudget.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * Load available categories from Firestore and populate spinner
     */
    private fun loadCategories() {
        db.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                val categories = mutableListOf<String>()
                for (document in result) {
                    categories.add(document.getString("name") ?: "")
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategoryBudget.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Save a new budget goal to Firestore for the current user
     */
    private fun saveBudgetGoal() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val category = spinnerCategoryBudget.selectedItem.toString()
        val month = spinnerMonth.selectedItem.toString()
        val minGoal = seekBarMinBudget.progress
        val maxGoal = seekBarMaxBudget.progress

        val budgetGoal = hashMapOf(
            "userEmail" to userEmail,
            "category" to category,
            "month" to month,
            "minGoal" to minGoal,
            "maxGoal" to maxGoal
        )

        // Use a document name like userEmail_category_month to avoid duplicates:
        val docId = "${userEmail}_${category}_$month"

        db.collection("budget_goals")
            .document(docId)
            .set(budgetGoal)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Budget goal saved!", Toast.LENGTH_SHORT).show()

                // Clear fields after saving:
                spinnerCategoryBudget.setSelection(0)
                spinnerMonth.setSelection(0)
                seekBarMinBudget.progress = 0
                seekBarMaxBudget.progress = 0
                tvMinBudget.text = "Min Budget: R0"
                tvMaxBudget.text = "Max Budget: R0"

                // Refresh the budget goals list after saving
                loadBudgetGoals()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error saving budget goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Load the user's saved budget goals from Firestore and display them in RecyclerView
     */
    private fun loadBudgetGoals() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        db.collection("budget_goals")
            .whereEqualTo("userEmail", userEmail)
            .get()
            .addOnSuccessListener { result ->
                budgetGoalsList.clear()
                for (document in result) {
                    val category = document.getString("category") ?: ""
                    val month = document.getString("month") ?: ""
                    val minGoal = (document.getLong("minGoal") ?: 0L).toInt()
                    val maxGoal = (document.getLong("maxGoal") ?: 0L).toInt()

                    val budgetGoal = BudgetGoal(category, month, minGoal, maxGoal)
                    budgetGoalsList.add(budgetGoal)
                }
                budgetGoalAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading budget goals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
