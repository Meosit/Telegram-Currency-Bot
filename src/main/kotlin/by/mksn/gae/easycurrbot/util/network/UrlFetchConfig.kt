package by.mksn.gae.easycurrbot.util.network

import com.google.appengine.api.urlfetch.ResponseTooLargeException
import com.google.appengine.api.urlfetch.URLFetchService
import io.ktor.client.engine.HttpClientEngineConfig
import javax.net.ssl.SSLHandshakeException

class UrlFetchConfig : HttpClientEngineConfig() {

    /**
     * If `true`, [URLFetchService] will truncate large responses and return them without error.
     * [ResponseTooLargeException] will be thrown otherwise.
     */
    var allowTruncate: Boolean = false
    /**
     * If `true`, [URLFetchService] operation will follow redirects.
     */
    var followRedirects: Boolean = false
    /**
     * If `true`, [URLFetchService] operation will, if using an HTTPS connection, instruct the application to
     * send a request to the server only if the certificate is valid and signed by a trusted certificate
     * authority (CA), and also includes a hostname that matches the certificate. If the certificate validation
     * fails, a [SSLHandshakeException] exception is thrown. HTTP connections are unaffected by this option.
     */
    var validateCertificate: Boolean = false

    /**
     * Interval between response polling attempts in milliseconds
     */
    var responsePollingInterval: Long = 100

}
