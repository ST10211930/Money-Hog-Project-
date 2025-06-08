package vcmsa.projects.moneyhog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore

class AddCategoryFragment : Fragment() {

    private lateinit var etCategoryName: EditText
    private lateinit var btnSaveCategory: Button
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etCategoryName = view.findViewById(R.id.etCategoryName)
        btnSaveCategory = view.findViewById(R.id.btnSaveCategory)

        btnSaveCategory.setOnClickListener {
            val categoryName = etCategoryName.text.toString().trim()

            if (categoryName.isNotEmpty()) {
                val category = hashMapOf(
                    "name" to categoryName
                )

                db.collection("categories")
                    .add(category)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Category saved!", Toast.LENGTH_SHORT).show()
                        etCategoryName.text.clear()
                        findNavController().popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error saving category: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
