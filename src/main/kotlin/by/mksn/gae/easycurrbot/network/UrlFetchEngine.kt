package by.mksn.gae.easycurrbot.network

import com.google.appengine.api.urlfetch.*
import io.ktor.client.call.UnsupportedContentTypeException
import io.ktor.client.engine.HttpClientJvmEngine
import io.ktor.client.engine.mergeHeaders
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.date.GMTDate
import io.ktor.util.toByteArray
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.writer
import java.net.URL
import kotlin.coroutines.CoroutineContext

class UrlFetchEngine(override val config: UrlFetchConfig) : HttpClientJvmEngine("ktor-urlfetch") {

    private val urlFetchService: URLFetchService by lazy {
        val urlFetchService = URLFetchServiceFactory.getURLFetchService()
        urlFetchService
    }

    @KtorExperimentalAPI
    @Suppress("BlockingMethodInNonBlockingContext")
    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {

        val requestTime = GMTDate()
        val callContext = createCallContext()

        val url = URL(data.url.toString())

        val fetchOptions = FetchOptions.Builder.withDefaults().apply {
            if (config.allowTruncate) allowTruncate() else disallowTruncate()
            if (config.followRedirects) followRedirects() else doNotFollowRedirects()
            if (config.validateCertificate) validateCertificate() else doNotValidateCertificate()
        }

        val httpRequest = HTTPRequest(url, HTTPMethod.valueOf(data.method.value), fetchOptions)
        with(httpRequest) {
            mergeHeaders(data.headers, data.body) { key, value -> addHeader(HTTPHeader(key, value)) }
            payload = data.body.toByteArray(callContext)
        }

        val httpResponse = urlFetchService.fetch(httpRequest)

        return HttpResponseData(HttpStatusCode.fromValue(httpResponse.responseCode),
                requestTime,
                Headers.build { httpResponse.headersUncombined.forEach { append(it.name, it.value) } },
                HttpProtocolVersion.HTTP_1_1,
                ByteReadChannel(httpResponse.content),
                callContext
        )
    }


}

@KtorExperimentalAPI
private suspend fun OutgoingContent.toByteArray(callContext: CoroutineContext) = when (this) {
    is OutgoingContent.NoContent -> null
    is OutgoingContent.ReadChannelContent -> readFrom().toByteArray()
    is OutgoingContent.ByteArrayContent -> bytes()
    is OutgoingContent.WriteChannelContent -> GlobalScope.writer(callContext) { writeTo(channel) }.channel.toByteArray()
    else -> throw UnsupportedContentTypeException(this)
}
