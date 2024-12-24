package vn.edu.hust.roomexample

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StudentDao {

  // Lấy tất cả sinh viên
  @Query("SELECT * FROM students")
  fun getAllStudents(): LiveData<List<Student>>

  // Tìm sinh viên theo MSSV
  @Query("SELECT * FROM students WHERE mssv = :mssv")
  fun findStudentById(mssv: String): List<Student>

  // Tìm sinh viên theo tên (phân biệt chữ hoa chữ thường)
  @Query("SELECT * FROM students WHERE hoten LIKE :name")
  fun findStudentByName(name: String): List<Student>

  // Thêm sinh viên mới, trả về ID
  @Insert
  fun insertStudent(student: Student): Long

  // Thêm sinh viên mới hoặc thay thế nếu có xung đột
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(student: Student)

  // Xóa sinh viên
  @Delete
  suspend fun delete(student: Student)

  // Xóa nhiều sinh viên theo MSSV
  @Query("DELETE FROM students WHERE mssv IN (:mssvList)")
  suspend fun deleteMultiple(mssvList: List<String>)

  // Tìm kiếm sinh viên theo MSSV hoặc họ tên (có thể tìm kiếm theo phần chữ của MSSV hoặc tên)
  @Query("SELECT * FROM students WHERE mssv LIKE :query OR hoten LIKE :query")
  fun search(query: String): LiveData<List<Student>>
}
