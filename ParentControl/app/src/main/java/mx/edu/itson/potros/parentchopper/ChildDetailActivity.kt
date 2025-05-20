package mx.edu.itson.potros.parentchopper

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import mx.edu.itson.potros.parentchopper.models.ChildDetail
import mx.edu.itson.potros.parentchopper.models.Grade
import mx.edu.itson.potros.parentchopper.models.Task
import mx.edu.itson.potros.parentchopper.models.Work

class ChildDetailActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var detailAdapter: ChildDetailAdapter
    private val detailList = mutableListOf<ChildDetail>() // Puedes modificar para usar un modelo común si quieres

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_detail)

        val name = intent.getStringExtra("name") ?: ""
        val photoUrl = intent.getStringExtra("photoUrl") ?: ""
        val childId = intent.getStringExtra("childId") ?: ""

        val nameText = findViewById<TextView>(R.id.detailName)
        val photoImage = findViewById<ImageView>(R.id.detailPhoto)
        val recyclerView = findViewById<RecyclerView>(R.id.detailRecyclerView)

        nameText.text = name
        Glide.with(this).load(photoUrl).into(photoImage)

        database = FirebaseDatabase.getInstance().reference

        detailAdapter = ChildDetailAdapter(detailList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = detailAdapter
        insertDummyData(childId)
        loadChildDetails(childId)

        loadChildDetails(childId)
    }

    private fun loadChildDetails(childId: String) {
        detailList.clear()

        // 1. Consultar tareas asignadas a este alumno
        database.child("tasks")
            .orderByChild("studentIds/$childId")
            .equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(tasksSnapshot: DataSnapshot) {
                    for (taskSnap in tasksSnapshot.children) {
                        val task = taskSnap.getValue(Task::class.java)
                        if (task != null) {
                            detailList.add(
                                ChildDetail(
                                    id = task.id,
                                    childId = childId,
                                    type = "Tarea",
                                    subject = task.subject,
                                    title = task.title,
                                    detailedInfo = "Instrucciones:\n${task.instructions}\nFecha de entrega: ${task.dueDate}"
                                )
                            )
                        }
                    }
                    // 2. Consultar calificaciones para este alumno
                    database.child("grades")
                        .orderByChild("studentId")
                        .equalTo(childId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(gradesSnapshot: DataSnapshot) {
                                for (gradeSnap in gradesSnapshot.children) {
                                    val grade = gradeSnap.getValue(Grade::class.java)
                                    if (grade != null) {
                                        detailList.add(
                                            ChildDetail(
                                                id = grade.id,
                                                childId = childId,
                                                type = "Calificación",
                                                subject = "Resumen general",
                                                title = "Promedio: ${grade.average}",
                                                detailedInfo = """
                                                    Tareas entregadas: ${grade.tasksDelivered}
                                                    Trabajos realizados: ${grade.worksDone}
                                                    Calificaciones de exámenes: ${grade.examGrades.joinToString()}
                                                """.trimIndent()
                                            )
                                        )
                                    }
                                }

                                // 3. Consultar trabajos para este alumno
                                database.child("works")
                                    .orderByChild("studentId")
                                    .equalTo(childId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(worksSnapshot: DataSnapshot) {
                                            for (workSnap in worksSnapshot.children) {
                                                val work = workSnap.getValue(Work::class.java)
                                                if (work != null) {
                                                    detailList.add(
                                                        ChildDetail(
                                                            id = work.id,
                                                            childId = childId,
                                                            type = "Trabajo",
                                                            subject = work.subject,
                                                            title = work.instructions,
                                                            detailedInfo = work.details
                                                        )
                                                    )
                                                }
                                            }
                                            // Finalmente actualizar el adapter
                                            detailAdapter.notifyDataSetChanged()
                                        }

                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
    private fun insertDummyData(childId: String) {
        val tasksRef = database.child("tasks")
        val gradesRef = database.child("grades")
        val worksRef = database.child("works")

        // Dummy task para este alumno
        val taskId = tasksRef.push().key!!
        val dummyTask = Task(
            id = taskId,
            title = "Ejercicios Álgebra",
            instructions = "Resolver los ejercicios 1-10 de la página 45",
            subject = "Matemáticas",
            dueDate = "2025-05-20",
            studentIds = mapOf(childId to true)
        )
        tasksRef.child(taskId).setValue(dummyTask)

        // Dummy grade para este alumno
        val gradeId = gradesRef.push().key!!
        val dummyGrade = Grade(
            id = gradeId,
            studentId = childId,
            average = 8.7,
            tasksDelivered = 4,
            worksDone = 2,
            examGrades = listOf(8.5, 9.0)
        )
        gradesRef.child(gradeId).setValue(dummyGrade)

        // Dummy work para este alumno
        val workId = worksRef.push().key!!
        val dummyWork = Work(
            id = workId,
            subject = "Matemáticas",
            instructions = "Trabajo sobre multiplicación",
            details = "Se resolvieron 5 de 10 ejercicios correctamente",
            studentId = childId
        )
        worksRef.child(workId).setValue(dummyWork)
    }

}
