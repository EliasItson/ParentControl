package mx.edu.itson.potros.parentchopper.models

data class Teacher(
    val id: String = "",
    val name: String = "",
    val subject: String = "",
    val students: Map<String, Boolean> = emptyMap()
)
