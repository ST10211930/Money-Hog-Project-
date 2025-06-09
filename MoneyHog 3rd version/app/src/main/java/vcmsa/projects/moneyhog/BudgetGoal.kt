package vcmsa.projects.moneyhog

data class BudgetGoal(
    val category: String,
    val month: String,
    val minGoal: Int,
    val maxGoal: Int
)
