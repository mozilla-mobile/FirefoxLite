package org.mozilla.rocket.content.news.data.dailyhunt

import android.util.Base64
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class DailyHuntUtils {

    companion object {

        @Throws(UnsupportedEncodingException::class, SignatureException::class)
        internal fun generateSignature(secretKey: String, httpMethod: String, queryParams: Map<String, String>): String {
            val generateSignatureBase = generateSignatureBase(queryParams)
            return calculateRFC2104HMAC(generateSignatureBase + httpMethod, secretKey)
        }

        @Throws(UnsupportedEncodingException::class)
        private fun generateSignatureBase(queryParams: Map<String, String>): String {
            val signatureBaseBuffer = StringBuilder()
            if (queryParams.isNotEmpty()) {

                // Sort all the request query parameters lexicographically, sorted on encoded key
                val encodedAndSortedQueryParams = TreeMap<String, String>()
                for (key in queryParams.keys) {
                    encodedAndSortedQueryParams[URLEncoder.encode(key, "UTF-8")] = URLEncoder.encode(queryParams[key], "UTF-8")
                }
                // Append all the key=value with “&” as separator if more than one key=value is present
                encodedAndSortedQueryParams.entries.map {
                    signatureBaseBuffer.append("${it.key}=${it.value}&")
                }
                // remove the last &
                val length = signatureBaseBuffer.length
                signatureBaseBuffer.deleteCharAt(length - 1)
            }

            // Append the UPPERCASE(http method)
            return signatureBaseBuffer.toString()
        }

        @Throws(SignatureException::class)
        private fun calculateRFC2104HMAC(data: String, key: String): String {
            return try {
                // get an hmac_sha1 key from the raw key bytes
                val signingKey = SecretKeySpec(key.toByteArray(), "HmacSHA1")
                // get an hmac_sha1 Mac instance and initialize with the signing key
                val mac = Mac.getInstance("HmacSHA1")
                mac.init(signingKey)
                // compute the hmac on input data bytes
                val rawHmac = mac.doFinal(data.toByteArray())
                // base64-encode the hmac
                Base64.encodeToString(rawHmac, Base64.DEFAULT)
            } catch (e: InvalidKeyException) {
                val message = "Failed to generate HMAC as key is invalid"
                throw SignatureException(message, e)
            } catch (e: NoSuchAlgorithmException) {
                val message = "Failed to generate HMAC as encoding algorithm does not exists"
                throw SignatureException(message, e)
            }
        }
    }
}