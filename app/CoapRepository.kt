package com.example.coap_iot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.CoapHandler
import org.eclipse.californium.core.CoapObserveRelation
import org.eclipse.californium.core.coap.MediaTypeRegistry

class CoapRepository {

    private var observeRelation: CoapObserveRelation? = null
    private var observeClient: CoapClient? = null

    private fun buildUri(host: String, path: String): String {
        return "coap://$host/$path"
    }

    suspend fun get(host: String, path: String): String = withContext(Dispatchers.IO) {
        val client = CoapClient(buildUri(host, path))
        client.timeout = 3000L
        try {
            val response = client.get()
            response?.responseText ?: "Sem resposta."
        } catch (e: Exception) {
            "Erro GET: ${e.message}"
        } finally {
            client.shutdown()
        }
    }

    suspend fun postJson(host: String, path: String, payload: String): String = withContext(Dispatchers.IO) {
        val client = CoapClient(buildUri(host, path))
        client.timeout = 3000L
        try {
            val response = client.post(payload, MediaTypeRegistry.APPLICATION_JSON)
            response?.responseText ?: "Sem resposta."
        } catch (e: Exception) {
            "Erro POST: ${e.message}"
        } finally {
            client.shutdown()
        }
    }

    fun startObserveCpu(
        host: String,
        onUpdate: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopObserve()

        observeClient = CoapClient(buildUri(host, "system/cpu"))
        observeClient?.timeout = 3000L

        observeRelation = observeClient?.observe(
            object : CoapHandler {
                override fun onLoad(response: org.eclipse.californium.core.CoapResponse?) {
                    onUpdate(response?.responseText ?: "Observe sem payload.")
                }

                override fun onError() {
                    onError("Erro no Observe.")
                }
            }
        )
    }

    fun stopObserve() {
        observeRelation?.proactiveCancel()
        observeRelation = null
        observeClient?.shutdown()
        observeClient = null
    }
}