package vcmsa.projects.moneyhog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController

class ExpensesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_expenses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAddExpense = view.findViewById<Button>(R.id.btnAddExpense)
        val btnAddCategory = view.findViewById<Button>(R.id.btnAddCategory)
        val btnViewExpenses = view.findViewById<Button>(R.id.btnViewExpenses)

        btnAddExpense.setOnClickListener {
            findNavController().navigate(R.id.addExpenseFragment)
        }

        btnAddCategory.setOnClickListener {
            findNavController().navigate(R.id.addCategoryFragment)
        }

        btnViewExpenses.setOnClickListener {
            btnViewExpenses.setOnClickListener {
                findNavController().navigate(R.id.viewExpensesFragment)
            }

        }
    }
}
