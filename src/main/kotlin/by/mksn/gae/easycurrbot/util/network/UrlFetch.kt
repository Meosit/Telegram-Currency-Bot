package by.mksn.gae.easycurrbot.util.network

import io.ktor.client.HttpClientEngineContainer
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory


object UrlFetch : HttpClientEngineFactory<UrlFetchConfig> {
    override fun create(block: UrlFetchConfig.() -> Unit): HttpClientEngine =
            UrlFetchEngine(UrlFetchConfig().apply(block))
}

class UrlFetchEngineContainer : HttpClientEngineContainer {
    override val factory: HttpClientEngineFactory<*> = UrlFetch
}
