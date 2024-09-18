package com.example.mailingr.preference
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mailingr.R
import com.example.mailingr.data.Contact
import com.example.mailingr.data.ContactAdapter
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private val contacts = mutableListOf<Contact>()
    private lateinit var spinnerList: Spinner
    private val SMS_PERMISSION_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        spinnerList = findViewById(R.id.spinnerList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter(contacts)
        recyclerView.adapter = contactAdapter

        // Загрузка списка групп из Firebase
        loadGroupNames()
        checkAndRequestPermissions()
        // Обработка выбора группы
        spinnerList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGroup = spinnerList.selectedItem.toString()
                if (selectedGroup != "Выбор") { // Игнорируем выбор заглушки
                    saveUserChoice(selectedGroup)  // Сохраняем выбор
                    fetchContacts(selectedGroup)   // Загружаем контакты для выбранной группы
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ничего не делать
            }
        }

    }

    private fun loadGroupNames() {
        val database = FirebaseDatabase.getInstance()
        val groupsRef = database.getReference("contacts")

        groupsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupNames = mutableListOf("Выбор") // Добавляем заглушку
                for (groupSnapshot in snapshot.children) {
                    groupNames.add(groupSnapshot.key ?: continue)
                }
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, groupNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerList.adapter = adapter

                // Восстанавливаем сохраненный выбор пользователя
                val savedGroup = loadSavedGroup()
                savedGroup?.let {
                    val position = adapter.getPosition(it)
                    if (position != -1) {
                        spinnerList.setSelection(position)
                        fetchContacts(it)  // Загружаем контакты для сохраненной группы
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load group names", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchContacts(group: String) {
        val database = FirebaseDatabase.getInstance()
        val contactsRef = database.getReference("contacts/$group")

        contactsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contacts.clear()
                for (contactSnapshot in snapshot.children) {
                    val contact = contactSnapshot.getValue(Contact::class.java)
                    contact?.let {
                        contacts.add(it)
                    }
                }
                contactAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибки
            }
        })

        contactsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val contact = snapshot.getValue(Contact::class.java)
                contact?.let {
                    if (contact.send == "Y") {
                        val contactId = snapshot.key // Получение ключа узла
                        contactId?.let { id ->
                            sendSmsToContacts(contact.phone, contact.message, id, group)
                        }
                    }
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Не нужно ничего делать здесь, так как мы уже обрабатываем добавление в onDataChange
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Обработка удаления, если нужно
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Обработка перемещения, если нужно
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибки
            }
        })
    }
    private fun saveUserChoice(selectedGroup: String) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selectedGroup", selectedGroup)
        editor.apply()  // Сохраняем выбор
    }
    private fun loadSavedGroup(): String? {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("selectedGroup", null)
    }

    private fun sendSmsToContacts(phoneNumber: String?, message: String?, contactId: String, group: String) {
        if (phoneNumber != null && message != null) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    val smsManager = SmsManager.getDefault()
                    try {
                        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                        Toast.makeText(this, "Message sent to $phoneNumber", Toast.LENGTH_SHORT).show()
                        Log.d("SMS", "Message sent to $phoneNumber")

                        // Получаем текущую дату и время
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val currentDate = dateFormat.format(System.currentTimeMillis())

                        // Обновляем дату отправки и статус отправки в Firebase
                        updateContactInFirebase(contactId, currentDate, "N", group)

                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to send message to $phoneNumber", Toast.LENGTH_SHORT).show()
                        Log.e("SMS", "Failed to send message to $phoneNumber", e)
                    }
                }, 5000) // Задержка 5 секунд перед отправкой
            } else {
                Toast.makeText(this, "SMS permission is required", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions() // Запросите разрешение снова
            }
        }
    }

    private fun updateContactInFirebase(contactId: String, sendDate: String, sendStatus: String, group: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("contacts")
            .child(group)
            .child(contactId)

        val updates = mapOf<String, Any>(
            "sendDate" to sendDate,
            "send" to sendStatus // Обновляем статус отправки
        )

        databaseReference.updateChildren(updates).addOnSuccessListener {
            Log.d("Firebase", "Contact updated successfully")
            Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e("Firebase", "Failed to update contact", e)
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Разрешение отклонено
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
