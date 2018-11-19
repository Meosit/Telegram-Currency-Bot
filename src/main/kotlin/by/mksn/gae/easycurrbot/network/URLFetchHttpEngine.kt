package by.mksn.gae.easycurrbot.network

import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.HttpEngineCall
import io.ktor.client.call.UnsupportedContentTypeException
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.mergeHeaders
import io.ktor.client.request.DefaultHttpRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.util.cio.toByteReadChannel
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.DefaultDispatcher
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author Mikhail Snitavets
 */
class URLFetchHttpEngine(override val config: HttpClientEngineConfig) : HttpClientEngine {

    override val dispatcher: CoroutineDispatcher
        get() = DefaultDispatcher

    override fun close() { }

    override suspend fun execute(call: HttpClientCall, data: HttpRequestData): HttpEngineCall {
        val request = DefaultHttpRequest(call, data)
        val requestTime = GMTDate()

        val url = URL(request.url.toString())
        val connection = url.openConnection() as HttpURLConnection
        data.url.parameters.isEmpty()
        // request
        with(connection) {
            requestMethod = request.method.value
            instanceFollowRedirects = false
            mergeHeaders(request.headers, request.content) { key, value ->
                setRequestProperty(key, value)
            }
            useCaches = false
            request.content.write(connection)
        }

        // response
        val statusCode = HttpStatusCode.fromValue(connection.responseCode)
        val byteReadChannel = if (statusCode.isSuccess())
            connection.inputStream.toByteReadChannel()
        else connection.errorStream.toByteReadChannel()

        return HttpEngineCall(request, URLFetchHttpResponse(
                status = statusCode,
                content = byteReadChannel,
                headers = connection.ktorHeaders,
                call = call,
                requestTime = requestTime
        ))
    }

    private val HttpURLConnection.ktorHeaders get() = Headers.build {
        for ((key, valuesList) in headerFields)
            valuesList.forEach { append(key, it) }
    }

    private fun OutgoingContent.write(connection: HttpURLConnection) {
        when(this) {
            is OutgoingContent.NoContent -> {}
            is OutgoingContent.ByteArrayContent -> {
                connection.doOutput = true
                DataOutputStream(connection.outputStream).use {
                    it.write(bytes())
                    it.flush()
                }
            }
            else -> throw UnsupportedContentTypeException(this)
        }
    }
}