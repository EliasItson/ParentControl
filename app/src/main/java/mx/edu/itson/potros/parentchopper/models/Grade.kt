package mx.edu.itson.potros.parentchopper.models

data class Grade(
    val id: String = "",
    val studentId: String = "",
    val average: Double = 0.0,
    val tasksDelivered: Int = 0,
    val worksDone: Int = 0,
    val examGrades: List<Double> = emptyList()
)