package by.mksn.gae.easycurrbot.network

import com.google.appengine.api.urlfetch.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import java.net.*
import kotlin.coroutines.*

class UrlFetchEngine(override val config: UrlFetchConfig) : HttpClientJvmEngine("ktor-urlfetch") {

    private val urlFetchService: URLFetchService by lazy {
        val urlFetchService = URLFetchServiceFactory.getURLFetchService()
        urlFetchService
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @InternalAPI
    override suspend fun execute(call: HttpClientCall, data: HttpRequestData): HttpEngineCall {
        val request = DefaultHttpRequest(call, data)
        val requestTime = GMTDate()
        val callContext = createCallContext()

        val url = URL(request.url.toString())

        val fetchOptions = FetchOptions.Builder.withDefaults().apply {
            if (config.allowTruncate) allowTruncate() else disallowTruncate()
            if (config.followRedirects) followRedirects() else doNotFollowRedirects()
            if (config.validateCertificate) validateCertificate() else doNotValidateCertificate()
        }

        val httpRequest = HTTPRequest(url, HTTPMethod.valueOf(request.method.value), fetchOptions)
        with(httpRequest) {
            mergeHeaders(request.headers, request.content) { key, value -> addHeader(HTTPHeader(key, value)) }
            payload = request.content.toByteArray(callContext)
        }

        val httpResponse = urlFetchService.fetch(httpRequest)

        return HttpEngineCall(request, UrlFetchResponse(
            HttpStatusCode.fromValue(httpResponse.responseCode),
            call,
            requestTime,
            callContext,
            ByteReadChannel(httpResponse.content),
            Headers.build { httpResponse.headersUncombined.forEach { append(it.name, it.value) } }
        ))
    }


}

private suspend fun OutgoingContent.toByteArray(callContext: CoroutineContext) = when (this) {
    is OutgoingContent.NoContent -> null
    is OutgoingContent.ReadChannelContent -> readFrom().toByteArray()
    is OutgoingContent.ByteArrayContent -> bytes()
    is OutgoingContent.WriteChannelContent -> GlobalScope.writer(callContext) { writeTo(channel) }.channel.toByteArray()
    else -> throw UnsupportedContentTypeException(this)
}
