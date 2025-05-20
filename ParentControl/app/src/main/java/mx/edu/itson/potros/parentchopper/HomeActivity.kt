package mx.edu.itson.potros.parentchopper

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import mx.edu.itson.potros.parentchopper.models.Child
import mx.edu.itson.potros.parentchopper.models.Teacher
import android.widget.EditText
import androidx.core.app.NotificationCompat
import mx.edu.itson.potros.parentchopper.models.Grade


class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChildAdapter
    private val childrenList = mutableListOf<Child>()
    private var childIdMap = mutableMapOf<String, String>() // childId -> childName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        recyclerView = findViewById(R.id.childrenRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ChildAdapter(childrenList) { selectedChild ->
            val intent = Intent(this, ChildDetailActivity::class.java).apply {
                putExtra("name", selectedChild.name)
                putExtra("photoUrl", selectedChild.photoUrl)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        registerUserIfNeeded()
        insertDummyChildrenIfNone()
    }

    private fun registerUserIfNeeded() {
        val currentUserId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(currentUserId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val newUser = mapOf("id" to currentUserId, "name" to "Padre $currentUserId")
                    userRef.setValue(newUser)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al verificar usuario: ${error.message}")
            }
        })
    }

    private fun insertDummyChildrenIfNone() {
        val currentUserId = auth.currentUser?.uid ?: return
        val childrenRef = database.child("children")

        childrenRef.orderByChild("parentId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        val dummyChildren = listOf(
                            Child(
                                "child1",
                                "Juan Pérez",
                                "https://randomuser.me/api/portraits/men/1.jpg",
                                currentUserId
                            ),
                            Child(
                                "child2",
                                "María Gómez",
                                "https://randomuser.me/api/portraits/women/1.jpg",
                                currentUserId
                            )
                        )
                        val childIds = mutableListOf<String>()
                        dummyChildren.forEach { child ->
                            val childId = database.child("children").push().key!!
                            database.child("children").child(childId)
                                .setValue(child.copy(id = childId))
                            childIds.add(childId)
                        }

                        // Llamar después de agregar hijos
                        insertDummyTeachersIfNone(childIds)
                        insertDummyGrades(childIds)

                    }
                    loadChildren()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error al verificar hijos: ${error.message}")
                }
            })
    }

    private fun insertDummyGrades(childIds: List<String>) {
        val gradesRef = database.child("grades")

        val dummyGrades = listOf(
            Grade(
                id = "",
                studentId = childIds[0],
                average = 5.5,
                tasksDelivered = 4,
                worksDone = 3,
                examGrades = listOf(5.0, 6.0)
            ),
            Grade(
                id = "",
                studentId = childIds[1],
                average = 8.0,
                tasksDelivered = 5,
                worksDone = 5,
                examGrades = listOf(8.0, 9.0)
            )
        )

        dummyGrades.forEach { grade ->
            val gradeId = gradesRef.push().key!!
            gradesRef.child(gradeId).setValue(grade.copy(id = gradeId))
        }
    }


    private fun loadChildren() {
        val currentUserId = auth.currentUser?.uid ?: return
        val childrenRef = database.child("children")

        childrenRef.orderByChild("parentId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childrenList.clear()
                    childIdMap.clear()
                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        if (child != null) {
                            childrenList.add(child)
                            childIdMap[child.id] = child.name
                        }
                    }
                    adapter.notifyDataSetChanged()
                    listenForLowGrades()
                    fetchAndDisplayTeachers() // Llamamos después de cargar hijos
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error al cargar hijos: ${error.message}")
                }
            })
    }

    private fun fetchAndDisplayTeachers() {
        val teacherContainer = findViewById<LinearLayout>(R.id.cardContainer)

        database.child("teachers").get().addOnSuccessListener { snapshot ->
            teacherContainer.removeAllViews()
            for (teacherSnap in snapshot.children) {
                val id = teacherSnap.key ?: continue
                val teacher = teacherSnap.getValue(Teacher::class.java)?.copy(id = id) ?: continue

                // Filtramos solo los profesores que tienen al menos un hijo del padre actual
                val commonChildren = teacher.students.keys.filter { childIdMap.containsKey(it) }
                if (commonChildren.isEmpty()) continue

                val cardView = layoutInflater.inflate(R.layout.teacher_card, null)

                val tvName = cardView.findViewById<TextView>(R.id.tvTeacherName)
                val tvSubject = cardView.findViewById<TextView>(R.id.tvTeacherSubject)
                val tvStudents = cardView.findViewById<TextView>(R.id.tvTeacherStudents)

                tvName.text = teacher.name
                tvSubject.text = "Materia: ${teacher.subject}"
                val studentNames = commonChildren.mapNotNull { childIdMap[it] }
                tvStudents.text = "Alumnos: ${studentNames.joinToString()}"

                cardView.setOnClickListener {
                    showTeacherDialog(teacher, studentNames)
                }

                teacherContainer.addView(cardView)
            }
        }
    }

    private fun showTeacherDialog(teacher: Teacher, studentNames: List<String>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_teacher_detail, null)
        val chatIcon = dialogView.findViewById<ImageView>(R.id.ivChatIcon)

        chatIcon.setOnClickListener {
            showChatDialog(teacher)
        }

        dialogView.findViewById<TextView>(R.id.dialogTeacherName).text = teacher.name
        dialogView.findViewById<TextView>(R.id.dialogTeacherSubject).text =
            "Materia: ${teacher.subject}"
        dialogView.findViewById<TextView>(R.id.dialogTeacherStudents).text =
            "Alumnos: ${studentNames.joinToString()}"

        AlertDialog.Builder(this)
            .setTitle("Detalle del Profesor")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun insertDummyTeachersIfNone(childIds: List<String>) {
        val teachersRef = database.child("teachers")

        teachersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val dummyTeachers = listOf(
                        Teacher(
                            id = "t1",
                            name = "Prof. Laura Mendoza",
                            subject = "Matemáticas",
                            students = childIds.associateWith { true }
                        ),
                        Teacher(
                            id = "t2",
                            name = "Prof. Carlos Rivera",
                            subject = "Historia",
                            students = mapOf(childIds.first() to true)
                        )
                    )

                    dummyTeachers.forEach { teacher ->
                        val teacherId = teachersRef.push().key!!
                        teachersRef.child(teacherId).setValue(teacher.copy(id = teacherId))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al insertar maestros: ${error.message}")
            }
        })
    }

    private fun showChatDialog(teacher: Teacher) {
        val chatView = layoutInflater.inflate(R.layout.dialog_chat, null)
        val chatMessages = chatView.findViewById<TextView>(R.id.chatMessages)
        val chatInput = chatView.findViewById<EditText>(R.id.chatInput)
        val chatSendButton = chatView.findViewById<Button>(R.id.chatSendButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Chat con ${teacher.name}")
            .setView(chatView)
            .setNegativeButton("Cerrar", null)
            .create()

        val dummyMessages = StringBuilder("Profesor: ¡Hola! ¿En qué puedo ayudarte?\n")
        chatMessages.text = dummyMessages.toString()

        chatSendButton.setOnClickListener {
            val msg = chatInput.text.toString().trim()
            if (msg.isNotEmpty()) {
                dummyMessages.append("Tú: $msg\n")
                chatMessages.text = dummyMessages.toString()
                chatInput.setText("")
            }
        }

        dialog.show()
    }

    private fun listenForLowGrades() {
        val currentUserId = auth.currentUser?.uid ?: return
        val gradesRef = database.child("grades")

        gradesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val grade = snapshot.getValue(Grade::class.java) ?: return
                val childName = childIdMap[grade.studentId] ?: return

                if (grade.average < 6.0) {
                    val message =
                        "Tu hijo/a $childName tiene un promedio reprobatorio de ${grade.average}"

                    // Mostrar AlertDialog si la app está abierta
                    runOnUiThread {
                        AlertDialog.Builder(this@HomeActivity)
                            .setTitle("¡Calificación Baja!")
                            .setMessage(message)
                            .setPositiveButton("Aceptar", null)
                            .show()
                    }

                    // Mostrar notificación del sistema
                    showLowGradeNotification(childName, grade.average)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })


    }
    private fun showLowGradeNotification(childName: String, average: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "low_grade_channel"

        // Crear canal (solo una vez en Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Calificaciones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando un hijo/a reprueba"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_warning) // Asegúrate de tener un ícono válido en res/drawable
            .setContentTitle("¡Calificación Baja!")
            .setContentText("Tu hijo/a $childName tiene un promedio de $average")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(childName.hashCode(), notificationBuilder.build())
    }

}

