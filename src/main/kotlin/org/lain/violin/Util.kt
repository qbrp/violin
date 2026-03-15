package org.lain.violin

import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

fun isValidURL(url: String): Boolean {
    try {
        URI(url).toURL()
        return true
    } catch (e: MalformedURLException) {
        return false
    } catch (e: URISyntaxException) {
        return false
    }
}