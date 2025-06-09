package vcmsa.projects.moneyhog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.moneyhog.databinding.FragmentGraphBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class GraphFragment : Fragment() {

    private var _binding: FragmentGraphBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraphBinding.inflate(inflater, container, false)

        setupSpinner()
        return binding.root
    }

    private fun setupSpinner() {
        val periods = listOf("Last 7 Days", "Last 30 Days", "Last 90 Days")
        val spinner = binding.spinnerPeriod
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val days = when (pos) {
                    0 -> 7
                    1 -> 30
                    2 -> 90
                    else -> 7
                }
                loadChartData(days)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadChartData(days: Int) {
        val currentUser = auth.currentUser?.email ?: return
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = dateFormatter.format(calendar.time)

        db.collection("expenses")
            .whereEqualTo("userEmail", currentUser)
            .whereGreaterThanOrEqualTo("date", startDate)
            .get()
            .addOnSuccessListener { result ->
                val categoryTotals = mutableMapOf<String, Float>()

                for (doc in result) {
                    val category = doc.getString("category") ?: "Uncategorized"
                    val amount = doc.getDouble("amount")?.toFloat() ?: 0f
                    categoryTotals[category] = categoryTotals.getOrDefault(category, 0f) + amount
                }

                displayChart(categoryTotals)
            }
            .addOnFailureListener {
                // Handle failure (optional: show Toast or log)
            }
    }

    private fun displayChart(categoryTotals: Map<String, Float>) {
        val entries = ArrayList<Entry>()
        val minEntries = ArrayList<Entry>()
        val maxEntries = ArrayList<Entry>()

        val categories = categoryTotals.keys.toList().sorted()
        categories.forEachIndexed { index, category ->
            val amount = categoryTotals[category] ?: 0f
            entries.add(Entry(index.toFloat(), amount))
            minEntries.add(Entry(index.toFloat(), 50f)) // Your min goal
            maxEntries.add(Entry(index.toFloat(), 200f)) // Your max goal
        }

        val spendDataSet = LineDataSet(entries, "Amount Spent").apply {
            color = Color.BLUE
            circleRadius = 4f
            setDrawValues(true)
        }

        val minDataSet = LineDataSet(minEntries, "Min Goal").apply {
            color = Color.GREEN
            setDrawCircles(false)
            enableDashedLine(10f, 5f, 0f)
        }

        val maxDataSet = LineDataSet(maxEntries, "Max Goal").apply {
            color = Color.RED
            setDrawCircles(false)
            enableDashedLine(10f, 5f, 0f)
        }

        binding.lineChart.apply {
            data = LineData(spendDataSet, minDataSet, maxDataSet)
            xAxis.valueFormatter = CategoryAxisFormatter(categories)
            xAxis.granularity = 1f
            description.isEnabled = false
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
