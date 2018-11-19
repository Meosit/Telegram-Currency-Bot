package by.mksn.gae.easycurrbot.network

import io.ktor.client.HttpClientEngineContainer
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

/**
 * @author Mikhail Snitavets
 */
object URLFetch : HttpClientEngineFactory<HttpClientEngineConfig> {
    override fun create(block: HttpClientEngineConfig.() -> Unit) =
            URLFetchHttpEngine(HttpClientEngineConfig().apply(block))
}


class URLFetchHttpEngineContainer : HttpClientEngineContainer {
    override val factory: HttpClientEngineFactory<*> = URLFetch
}
