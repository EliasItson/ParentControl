package mx.edu.itson.potros.parentchopper.models

data class Task(
    val id: String = "",
    val title: String = "",
    val instructions: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val studentIds: Map<String, Boolean> = emptyMap() // IDs de alumnos asignados
)