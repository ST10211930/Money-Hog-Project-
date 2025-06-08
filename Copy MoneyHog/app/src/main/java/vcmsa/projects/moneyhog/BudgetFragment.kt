package vcmsa.projects.moneyhog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.*

class BudgetFragment : Fragment() {

    private lateinit var tvBudgetAmount: TextView
    private lateinit var seekBarBudget: SeekBar
    private lateinit var spinnerMonth: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvBudgetAmount = view.findViewById(R.id.tvBudgetAmount)
        seekBarBudget = view.findViewById(R.id.seekBarBudget)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        val btnSaveBudget = view.findViewById<Button>(R.id.btnSaveBudget)

        // Setup Month Spinner
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = adapter

        // Setup SeekBar listener
        seekBarBudget.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                tvBudgetAmount.text = "Budget: ${currencyFormat.format(progress)}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSaveBudget.setOnClickListener {
            val selectedMonth = spinnerMonth.selectedItem.toString()
            val budgetAmount = seekBarBudget.progress
            // Later → save selectedMonth and budgetAmount to Firestore

            Toast.makeText(
                requireContext(),
                "Budget saved: $selectedMonth → R$budgetAmount",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
