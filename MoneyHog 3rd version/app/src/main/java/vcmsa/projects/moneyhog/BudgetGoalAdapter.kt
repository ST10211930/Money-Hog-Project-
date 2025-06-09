package vcmsa.projects.moneyhog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BudgetGoalAdapter(private val budgetGoals: List<BudgetGoal>) :
    RecyclerView.Adapter<BudgetGoalAdapter.BudgetGoalViewHolder>() {

    class BudgetGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvMonth: TextView = itemView.findViewById(R.id.tvMonth)
        val tvMinGoal: TextView = itemView.findViewById(R.id.tvMinGoal)
        val tvMaxGoal: TextView = itemView.findViewById(R.id.tvMaxGoal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetGoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_goal, parent, false)
        return BudgetGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetGoalViewHolder, position: Int) {
        val budgetGoal = budgetGoals[position]
        holder.tvCategory.text = "Category: ${budgetGoal.category}"
        holder.tvMonth.text = "Month: ${budgetGoal.month}"
        holder.tvMinGoal.text = "Min: R${budgetGoal.minGoal}"
        holder.tvMaxGoal.text = "Max: R${budgetGoal.maxGoal}"
    }

    override fun getItemCount(): Int = budgetGoals.size
}
