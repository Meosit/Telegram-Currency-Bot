package by.mksn.gae.easycurrbot.network

import io.ktor.client.call.*
import io.ktor.client.response.*
import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.io.*
import kotlin.coroutines.*

internal class UrlFetchResponse(
    override val status: HttpStatusCode,
    override val call: HttpClientCall,
    override val requestTime: GMTDate,
    override val coroutineContext: CoroutineContext,
    override val content: ByteReadChannel,
    override val headers: Headers
) : HttpResponse {

    override val version = HttpProtocolVersion.HTTP_1_1

    override val responseTime: GMTDate = GMTDate()

    override fun close() {}
}
