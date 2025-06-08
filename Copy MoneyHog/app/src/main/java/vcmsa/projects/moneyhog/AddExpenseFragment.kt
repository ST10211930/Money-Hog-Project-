package vcmsa.projects.moneyhog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddExpenseFragment : Fragment() {

    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSaveExpense: Button
    private lateinit var btnChooseImage: Button
    private lateinit var etImageUrl: EditText
    private lateinit var imageViewExpense: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data!!.data
            imageViewExpense.setImageURI(selectedImageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etDate = view.findViewById(R.id.etDate)
        etStartTime = view.findViewById(R.id.etStartTime)
        etEndTime = view.findViewById(R.id.etEndTime)
        etDescription = view.findViewById(R.id.etDescription)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        btnSaveExpense = view.findViewById(R.id.btnSaveExpense)
        btnChooseImage = view.findViewById(R.id.btnChooseImage)
        imageViewExpense = view.findViewById(R.id.imageViewExpense)

        loadCategories()

        btnChooseImage.setOnClickListener {
            chooseImageFromGallery()
        }

        btnSaveExpense.setOnClickListener {
            saveExpense()
        }
    }

    private fun chooseImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

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
                spinnerCategory.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveExpense() {
        val userEmail = auth.currentUser?.email ?: "unknown"
        val date = etDate.text.toString().trim()
        val startTime = etStartTime.text.toString().trim()
        val endTime = etEndTime.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val category = spinnerCategory.selectedItem?.toString() ?: ""
        val pastedImageUrl = etImageUrl.text.toString().trim()
        val imageUrl = if (pastedImageUrl.isNotEmpty()) pastedImageUrl else selectedImageUri?.toString() ?: ""


        if (date.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty() && description.isNotEmpty() && category.isNotEmpty()) {
            val expense = hashMapOf(
                "userEmail" to userEmail,
                "date" to date,
                "startTime" to startTime,
                "endTime" to endTime,
                "description" to description,
                "category" to category,
                "imageUrl" to imageUrl
            )

            db.collection("expenses")
                .add(expense)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Expense saved!", Toast.LENGTH_SHORT).show()
                    clearFields()
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error saving expense: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields() {
        etDate.text.clear()
        etStartTime.text.clear()
        etEndTime.text.clear()
        etDescription.text.clear()
        imageViewExpense.setImageResource(android.R.drawable.ic_menu_gallery)
        selectedImageUri = null
    }
}
