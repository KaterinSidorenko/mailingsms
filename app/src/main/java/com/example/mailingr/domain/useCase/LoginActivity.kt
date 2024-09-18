package com.example.mailingr.domain.useCase

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mailingr.preference.MainActivity
import com.example.mailingr.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Инициализация Firebase Auth
        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailET)
        passwordEditText = findViewById(R.id.passET)
        loginButton = findViewById(R.id.buttonLog)
        registerTextView = findViewById(R.id.textView)

        // Проверяем, авторизован ли пользователь
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Пользователь уже вошел в систему, переходим на MainActivity
            navigateToMainActivity()
        }

        // Обработчик нажатия на кнопку входа
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        // Обработчик нажатия на текст регистрации
        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Вход успешен, можно переходить на MainActivity
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    // Ошибка входа
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Закрываем LoginActivity, чтобы пользователь не мог вернуться назад
    }
}