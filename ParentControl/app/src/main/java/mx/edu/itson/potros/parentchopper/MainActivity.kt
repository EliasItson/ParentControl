package mx.edu.itson.potros.parentchopper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            // Usuario autenticado → ir a la pantalla principal
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // Usuario no autenticado → ir a login
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}