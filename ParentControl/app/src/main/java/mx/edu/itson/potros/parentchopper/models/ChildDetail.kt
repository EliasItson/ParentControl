package mx.edu.itson.potros.parentchopper.models

data class ChildDetail(
    val id: String = "",
    val childId: String = "",
    val type: String = "", // Ejemplo: "Tarea", "Calificación", "Trabajo"
    val title: String = "",
    val subject: String = "",
    val description: String = "" ,
    val detailedInfo: String = ""   // Instrucciones completas, desglose de notas, etc.
)