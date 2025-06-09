package vcmsa.projects.moneyhog

data class Expense(
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val amount: Double = 0.0
)
