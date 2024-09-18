package com.example.mailingr.domain.useCase

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mailingr.preference.MainActivity
import com.example.mailingr.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var buttonLog: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Инициализация Firebase Auth
        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailET)
        passwordEditText = findViewById(R.id.passET)
        phoneEditText = findViewById(R.id.phoneNumberET)
        registerButton = findViewById(R.id.button)
        buttonLog = findViewById(R.id.buttonLog)


        buttonLog.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()

        }


        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val phoneNumber = phoneEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(email, password, phoneNumber)
            }
        }
    }

    private fun registerUser(email: String, password: String, phoneNumber: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Регистрация успешна
                    val user = auth.currentUser
                    user?.let {
                        // Сохранение номера телефона в Firebase Realtime Database
                        val userRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
                        val userData = User(user.uid, email, phoneNumber)
                        userRef.setValue(userData).addOnCompleteListener { saveTask ->
                            if (saveTask.isSuccessful) {
                                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                // Переход на главную активность
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish() // Закрываем RegisterActivity
                            } else {
                                Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    // Ошибка регистрации
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

data class User(val uid: String, val email: String, val phoneNumber: String)
