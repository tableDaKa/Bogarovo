package com.example.bogarovo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var phoneInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private var pendingPhone: String? = null
    private var pendingMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phoneInput = findViewById(R.id.editTextPhone)
        messageInput = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)

        sendButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            val message = messageInput.text.toString().trim()

            if (phone.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Vyplňte telefon i text SMS", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                sendSms(phone, message)
            } else {
                pendingPhone = phone
                pendingMessage = message
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SEND_SMS)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SEND_SMS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val phone = pendingPhone
                val message = pendingMessage
                if (phone != null && message != null) {
                    sendSms(phone, message)
                }
            } else {
                Toast.makeText(this, "Oprávnění pro SMS nebylo uděleno", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSms(phone: String, message: String) {
        SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
        Toast.makeText(this, "SMS odeslána", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_SEND_SMS = 100
    }
}
