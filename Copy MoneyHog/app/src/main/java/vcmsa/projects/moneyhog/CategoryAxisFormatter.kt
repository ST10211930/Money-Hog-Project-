package vcmsa.projects.moneyhog

import com.github.mikephil.charting.formatter.ValueFormatter

class CategoryAxisFormatter(private val categories: List<String>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        return if (index in categories.indices) categories[index] else ""
    }
}
