package com.fakesteps

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata as HealthMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.ZoneId

class MainActivity : AppCompatActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    private val permissions = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    private val requestPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(permissions)) {
                showToast("Permission mil gayi!")
            } else {
                showToast("Permission deny hui. Health Connect mein manually allow karo.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = HealthConnectClient.getSdkStatus(this)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            showToast("Health Connect available nahi hai")
            return
        }

        healthConnectClient = HealthConnectClient.getOrCreate(this)

        val etSteps = findViewById<EditText>(R.id.etSteps)
        val btnInject = findViewById<Button>(R.id.btnInject)

        btnInject.setOnClickListener {
            val steps = etSteps.text.toString().toLongOrNull()
            if (steps != null && steps > 0) {
                injectSteps(steps)
            } else {
                showToast("Sahi steps daalo")
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        CoroutineScope(Dispatchers.Main).launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (!granted.containsAll(permissions)) {
                requestPermissions.launch(permissions)
            }
        }
    }

    private fun injectSteps(steps: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = ZonedDateTime.now(ZoneId.systemDefault())
                val record = StepsRecord(
                    count = steps,
                    startTime = now.minusHours(1).toInstant(),
                    endTime = now.toInstant(),
                    startZoneOffset = now.offset,
                    endZoneOffset = now.offset,
                    metadata = HealthMetadata()
                )
                healthConnectClient.insertRecords(listOf(record))
                runOnUiThread { showToast("$steps steps inject ho gaye!") }
            } catch (e: Exception) {
                runOnUiThread { showToast("Error: ${e.message}") }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
