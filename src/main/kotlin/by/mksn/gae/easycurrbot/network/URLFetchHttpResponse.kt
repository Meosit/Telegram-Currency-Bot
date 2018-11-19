package by.mksn.gae.easycurrbot.network

import io.ktor.client.call.HttpClientCall
import io.ktor.client.response.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.io.ByteReadChannel

/**
 * @author Mikhail Snitavets
 */
class URLFetchHttpResponse(
        override val call: HttpClientCall,
        override val requestTime: GMTDate,
        override val headers: Headers,
        override val status: HttpStatusCode,
        override val content: ByteReadChannel
) : HttpResponse {
    override val executionContext: Job = Job()
    override val version = HttpProtocolVersion.HTTP_1_1
    override val responseTime: GMTDate = GMTDate()
    override fun close() {}
}