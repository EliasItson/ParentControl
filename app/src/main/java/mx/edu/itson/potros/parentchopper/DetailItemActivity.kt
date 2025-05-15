package mx.edu.itson.potros.parentchopper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import mx.edu.itson.potros.parentchopper.models.ChildDetail

class DetailItemActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage

    private val PICK_FILE_REQUEST = 1001
    private var fileUri: Uri? = null

    private lateinit var buttonAttachFile: Button
    private lateinit var buttonSubmitTask: Button
    private lateinit var buttonApproveWork: Button
    private lateinit var buttonDisapproveWork: Button

    private lateinit var detailId: String
    private lateinit var type: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_item)

        val titleText = findViewById<TextView>(R.id.detailTitle)
        val subjectText = findViewById<TextView>(R.id.detailSubject)
        val bodyText = findViewById<TextView>(R.id.detailBody)

        buttonAttachFile = findViewById(R.id.buttonAttachFile)
        buttonSubmitTask = findViewById(R.id.buttonSubmitTask)
        buttonApproveWork = findViewById(R.id.buttonApproveWork)
        buttonDisapproveWork = findViewById(R.id.buttonDisapproveWork)

        detailId = intent.getStringExtra("detailId") ?: return
        type = intent.getStringExtra("type") ?: ""

        val subject = intent.getStringExtra("subject") ?: ""
        val detailedInfo = intent.getStringExtra("detailedInfo") ?: ""

        titleText.text = type
        subjectText.text = subject
        bodyText.text = detailedInfo

        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        when (type) {
            "Tarea" -> {
                buttonAttachFile.visibility = View.VISIBLE
                buttonSubmitTask.visibility = View.VISIBLE

                buttonApproveWork.visibility = View.GONE
                buttonDisapproveWork.visibility = View.GONE

                buttonAttachFile.setOnClickListener { openFileChooser() }
                buttonSubmitTask.setOnClickListener { submitTask() }
            }

            "Trabajo" -> {
                buttonApproveWork.visibility = View.VISIBLE
                buttonDisapproveWork.visibility = View.VISIBLE

                buttonAttachFile.visibility = View.GONE
                buttonSubmitTask.visibility = View.GONE

                buttonApproveWork.setOnClickListener { updateWorkStatus(true) }
                buttonDisapproveWork.setOnClickListener { updateWorkStatus(false) }
            }

            else -> {
                // Oculta todos los botones si no es Tarea ni Trabajo
                buttonAttachFile.visibility = View.GONE
                buttonSubmitTask.visibility = View.GONE
                buttonApproveWork.visibility = View.GONE
                buttonDisapproveWork.visibility = View.GONE
            }
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "Selecciona un archivo"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            fileUri = data.data
            buttonAttachFile.text = "Archivo seleccionado"
        }
    }

    private fun submitTask() {
        if (fileUri == null) {
            Toast.makeText(this, "Por favor selecciona un archivo primero", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = storage.reference
            .child("task_submissions")
            .child("$detailId/${System.currentTimeMillis()}")

        val uploadTask = storageRef.putFile(fileUri!!)
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val updates = mapOf<String, Any>(
                    "completed" to true,
                    "submissionFileUrl" to uri.toString()
                )

                database.child("tasks").child(detailId).updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tarea enviada correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar tarea", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al subir archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateWorkStatus(approved: Boolean) {
        val updates = mapOf<String, Any>(
            "approved" to approved
        )

        database.child("works").child(detailId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, if (approved) "Trabajo aprobado" else "Trabajo desaprobado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar estado", Toast.LENGTH_SHORT).show()
            }
    }
}
