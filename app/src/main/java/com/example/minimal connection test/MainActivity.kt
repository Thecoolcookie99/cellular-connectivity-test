package com.example.minimalconnectiontest

import android.os.Bundle
import android.widget.*
import android.app.Activity
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var result: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Minimal Connection Test"
            textSize = 26f
        }
        val explanation = TextView(this).apply {
            text = "Sends very small probes. It does not run a speed test or download large files."
            textSize = 16f
            setPadding(0, 16, 0, 24)
        }
        button = Button(this).apply {
            text = "TEST CONNECTION"
            setOnClickListener { runTest() }
        }
        result = TextView(this).apply {
            textSize = 16f
            setPadding(0, 24, 0, 0)
            text = "Ready."
        }

        layout.addView(title)
        layout.addView(explanation)
        layout.addView(button)
        layout.addView(result)
        setContentView(layout)
    }

    private fun runTest() {
        button.isEnabled = false
        result.text = "Testing…\n\nTiny DNS, TCP and HTTPS probes will be attempted."
        thread {
            val out = StringBuilder()

            fun timed(name: String, block: () -> String): String {
                val start = System.nanoTime()
                return try {
                    val v = block()
                    val ms = (System.nanoTime() - start) / 1_000_000
                    "$name: OK (${ms} ms) — $v"
                } catch (e: Exception) {
                    val ms = (System.nanoTime() - start) / 1_000_000
                    "$name: FAILED (${ms} ms) — ${e.javaClass.simpleName}: ${e.message ?: "no response"}"
                }
            }

            out.append(timed("DNS") {
                val addresses = java.net.InetAddress.getAllByName("example.com")
                "${addresses.firstOrNull()?.hostAddress ?: "no address"}"
            }).append("\n\n")

            out.append(timed("TCP") {
                Socket().use { s ->
                    s.connect(InetSocketAddress("example.com", 443), 5000)
                    "port 443 reachable"
                }
            }).append("\n\n")

            out.append(timed("Tiny HTTPS") {
                val conn = (URL("https://example.com/").openConnection() as HttpsURLConnection)
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "HEAD"
                conn.setRequestProperty("Cache-Control", "no-cache")
                conn.connect()
                val code = conn.responseCode
                conn.disconnect()
                "HTTP $code"
            })

            runOnUiThread {
                result.text = out.toString()
                button.isEnabled = true
            }
        }
    }
}
