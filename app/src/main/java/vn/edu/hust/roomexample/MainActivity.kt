package vn.edu.hust.roomexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import vn.edu.hust.roomexample.databinding.ActivityMainBinding
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.*

// view model
class StudentViewModel(application: android.app.Application) :
  androidx.lifecycle.AndroidViewModel(application) {

  private val studentDao = StudentDatabase.getInstance(application).studentDao()

  // Sửa lại để allStudents trả về LiveData
  val allStudents: LiveData<List<Student>> = studentDao.getAllStudents() // Sửa từ List<Student> thành LiveData<List<Student>>

  fun searchStudents(query: String): LiveData<List<Student>> {
    return studentDao.search("%$query%")
  }

  fun insert(student: Student) {
      viewModelScope.launch(Dispatchers.IO) {
      studentDao.insert(student)
    }
  }

  fun delete(student: Student) {
    viewModelScope.launch(Dispatchers.IO) {
      studentDao.delete(student)
    }
  }

  fun deleteMultiple(mssvList: List<String>) {
    viewModelScope.launch(Dispatchers.IO) {
      studentDao.deleteMultiple(mssvList)
    }
  }
}

// Adapter
class StudentAdapter(
  private var students: List<Student>,
  private val onItemClick: (Student) -> Unit,
  private val onItemCheckedChange: (String, Boolean) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

  private val selectedItems = mutableSetOf<String>()

  fun getSelectedItems(): List<String> {
    return selectedItems.toList()
  }

  fun toggleCheckAll(isChecked: Boolean) {
    selectedItems.clear()
    if (isChecked) {
      students.forEach { selectedItems.add(it.mssv) }
    }
    notifyDataSetChanged()
  }

  inner class StudentViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val mssvText: android.widget.TextView = itemView.findViewById(R.id.text_mssv)
    private val hotenText: android.widget.TextView = itemView.findViewById(R.id.text_hoten)
    private val checkBox: android.widget.CheckBox = itemView.findViewById(R.id.checkbox_select)

    fun bind(student: Student) {
      mssvText.text = student.mssv
      hotenText.text = student.hoten
      checkBox.setOnCheckedChangeListener(null)
      checkBox.isChecked = selectedItems.contains(student.mssv)
      itemView.setOnClickListener { onItemClick(student) }
      checkBox.setOnCheckedChangeListener { _, isChecked ->
        onItemCheckedChange(student.mssv, isChecked)
        if (isChecked) {
          selectedItems.add(student.mssv)
        } else {
          selectedItems.remove(student.mssv)
        }
      }
    }
  }

  override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): StudentViewHolder {
    val view = android.view.LayoutInflater.from(parent.context)
      .inflate(R.layout.item_student, parent, false)
    return StudentViewHolder(view)
  }

  override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
    holder.bind(students[position])
  }

  override fun getItemCount(): Int = students.size

  fun updateList(newList: List<Student>) {
    students = newList
    notifyDataSetChanged()
  }
}


// MainActivity
class MainActivity : AppCompatActivity() {
  private lateinit var viewModel: StudentViewModel
  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: StudentAdapter
  private lateinit var searchView: SearchView
  private var isAllChecked = false // Biến trạng thái toggle Check All

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    viewModel = ViewModelProvider(this).get(StudentViewModel::class.java)

    recyclerView = findViewById(R.id.recyclerView)
    recyclerView.layoutManager = LinearLayoutManager(this)

    searchView = findViewById(R.id.search_view)

    adapter = StudentAdapter(emptyList(), this::onStudentClick, this::onStudentCheckChange)
    recyclerView.adapter = adapter

    viewModel.allStudents.observe(this, Observer { students ->
      adapter.updateList(students)
    })

    val btnInsert = findViewById<Button>(R.id.button_insert)
    val btnGetAll = findViewById<Button>(R.id.button_get_all)
    val btnDelete = findViewById<Button>(R.id.button_delete)
    val btnCheckAll = findViewById<Button>(R.id.button_get_all)
    btnCheckAll.setOnClickListener {
      isAllChecked = !isAllChecked
      adapter.toggleCheckAll(isAllChecked) // Gọi hàm toggleCheckAll trong adapter
      btnCheckAll.text = if (isAllChecked) "Uncheck All" else "Check All"
    }


    btnInsert.setOnClickListener {
      val mssv = findViewById<EditText>(R.id.edit_mssv).text.toString()
      val hoten = findViewById<EditText>(R.id.edit_hoten).text.toString()

      if (mssv.isNotEmpty() && hoten.isNotEmpty()) {
        val newStudent = Student(mssv = mssv, hoten = hoten)
        viewModel.insert(newStudent)
        Toast.makeText(this, "Student added!", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(this, "MSSV and Name are required!", Toast.LENGTH_SHORT).show()
      }
    }

    btnDelete.setOnClickListener {
      val selectedMSSV = adapter.getSelectedItems()
      if (selectedMSSV.isNotEmpty()) {
        viewModel.deleteMultiple(selectedMSSV)
        Toast.makeText(this, "Deleted selected students", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(this, "No students selected!", Toast.LENGTH_SHORT).show()
      }
    }

    // Gắn sự kiện click cho nút lấy tất cả sinh viên
    btnGetAll.setOnClickListener {
      Toast.makeText(this, "Get All Students Clicked", Toast.LENGTH_SHORT).show()
    }

    // Lắng nghe sự thay đổi trong SearchView
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String?): Boolean {
        return true
      }

      override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let {
          viewModel.searchStudents(it).observe(this@MainActivity, Observer { students ->
            adapter.updateList(students)
          })
        }
        return true
      }
    })
  }

  private fun onStudentClick(student: Student) {
    Toast.makeText(this, "Clicked: ${student.mssv}", Toast.LENGTH_SHORT).show()
  }

  private fun onStudentCheckChange(mssv: String, isChecked: Boolean) {
    Toast.makeText(this, "Checked: $mssv = $isChecked", Toast.LENGTH_SHORT).show()
  }
}
