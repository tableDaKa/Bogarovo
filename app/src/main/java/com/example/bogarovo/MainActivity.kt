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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phoneInput = findViewById(R.id.inputPhone)
        messageInput = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.buttonSend)

        sendButton.setOnClickListener {
            if (hasSmsPermission()) {
                sendSms()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.SEND_SMS),
                    REQUEST_SEND_SMS
                )
            }
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sendSms() {
        val phone = phoneInput.text.toString().trim()
        val message = messageInput.text.toString()
        if (phone.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Vyplňte telefon a text SMS", Toast.LENGTH_SHORT).show()
            return
        }
        val smsManager = getSystemService(SmsManager::class.java)
        smsManager.sendTextMessage(phone, null, message, null, null)
        Toast.makeText(this, "SMS odeslána", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SEND_SMS && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            sendSms()
        }
    }

    companion object {
        private const val REQUEST_SEND_SMS = 100
    }
}
