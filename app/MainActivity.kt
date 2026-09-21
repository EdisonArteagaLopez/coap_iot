package com.example.coap_iot

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etServerHost: EditText
    private lateinit var etActuatorMessage: EditText
    private lateinit var tvOutput: TextView
    private lateinit var tvObserveOutput: TextView
    private lateinit var btnObserveCpu: Button

    private val repo = CoapRepository()
    private var observeRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etServerHost = findViewById(R.id.etServerHost)
        etActuatorMessage = findViewById(R.id.etActuatorMessage)
        tvOutput = findViewById(R.id.tvOutput)
        tvObserveOutput = findViewById(R.id.tvObserveOutput)
        btnObserveCpu = findViewById(R.id.btnObserveCpu)

        val btnDiscover = findViewById<Button>(R.id.btnDiscover)
        val btnCpu = findViewById<Button>(R.id.btnCpu)
        val btnMemory = findViewById<Button>(R.id.btnMemory)
        val btnDisk = findViewById<Button>(R.id.btnDisk)
        val btnTemperature = findViewById<Button>(R.id.btnTemperature)
        val btnSendMessage = findViewById<Button>(R.id.btnSendMessage)
        val btnWriteLog = findViewById<Button>(R.id.btnWriteLog)

        btnDiscover.setOnClickListener {
            runGet(".well-known/core")
        }

        btnCpu.setOnClickListener {
            runGet("system/cpu")
        }

        btnMemory.setOnClickListener {
            runGet("system/memory")
        }

        btnDisk.setOnClickListener {
            runGet("system/disk")
        }

        btnTemperature.setOnClickListener {
            runGet("environment/random-temperature")
        }

        btnObserveCpu.setOnClickListener {
            toggleObserveCpu()
        }

        btnSendMessage.setOnClickListener {
            val payload = """
                {
                  "title": "Mensagem do Raspberry",
                  "message": "${escapeJson(etActuatorMessage.text.toString())}"
                }
            """.trimIndent()
            runPost("actuators/message", payload)
        }

        btnWriteLog.setOnClickListener {
            val payload = """
                {
                  "message": "${escapeJson(etActuatorMessage.text.toString())}"
                }
            """.trimIndent()
            runPost("actuators/log", payload)
        }
    }

    private fun serverHost(): String {
        return etServerHost.text.toString().trim()
    }

    private fun runGet(path: String) {
        lifecycleScope.launch {
            appendOutput("GET $path")
            val response = repo.get(serverHost(), path)
            appendOutput(response)
        }
    }

    private fun runPost(path: String, payload: String) {
        lifecycleScope.launch {
            appendOutput("POST $path")
            appendOutput("Payload: $payload")
            val response = repo.postJson(serverHost(), path, payload)
            appendOutput(response)
        }
    }

    private fun toggleObserveCpu() {
        if (!observeRunning) {
            observeRunning = true
            btnObserveCpu.text = "Parar Observe da CPU"
            tvObserveOutput.text = "Observe CPU: iniciando..."

            repo.startObserveCpu(
                host = serverHost(),
                onUpdate = { text ->
                    runOnUiThread {
                        tvObserveOutput.text = "Observe CPU:\n$text"
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        tvObserveOutput.text = error
                    }
                }
            )
        } else {
            observeRunning = false
            btnObserveCpu.text = "Iniciar Observe da CPU"
            repo.stopObserve()
            tvObserveOutput.text = "Observe CPU: parado"
        }
    }

    private fun appendOutput(text: String) {
        tvOutput.text = "${tvOutput.text}\n\n$text"
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    override fun onDestroy() {
        super.onDestroy()
        repo.stopObserve()
    }
}