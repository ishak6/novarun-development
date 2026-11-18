package com.mangalocalrunnerplus

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.security.auth.x500.X500Principal

/**
 * Creates and retains a device-local TLS identity in Android Keystore.
 *
 * The certificate is self-signed. TLS encrypts traffic, but browsers will not
 * automatically trust this certificate; users must verify its SHA-256
 * fingerprint before trusting/importing it on another device.
 */
data class LanTlsIdentity(
    val sslContext: SSLContext,
    val sha256Fingerprint: String,
    val enabledProtocols: List<String>
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val VALIDITY_DAYS = 3650L

        fun create(context: Context): LanTlsIdentity {
            // v2 replaces the original key, whose digest authorization was too
            // narrow for Android's TLS signer. Keep v1 until v2 is fully usable.
            val keyAlias = "novarun_${context.packageName}_lan_tls_identity_v2"
            val legacyAlias = "novarun_${context.packageName}_lan_tls_identity_v1"
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
            }

            if (!keyStore.containsAlias(keyAlias)) {
                val now = System.currentTimeMillis()
                val generator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    KEYSTORE_PROVIDER
                )
                val paddings = arrayOf(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                val spec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setKeySize(2048)
                    // TLS providers often pass an already-computed digest to
                    // Android Keystore. SHA-256 is also needed for the
                    // self-signed certificate created with this key.
                    .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(*paddings)
                    .setCertificateSubject(X500Principal("CN=NovaRun Local Server"))
                    .setCertificateSerialNumber(BigInteger.valueOf(now.coerceAtLeast(1L)))
                    .setCertificateNotBefore(Date(now - 60_000L))
                    .setCertificateNotAfter(Date(now + VALIDITY_DAYS * 24L * 60L * 60L * 1000L))
                    .build()
                generator.initialize(spec)
                generator.generateKeyPair()
            }

            val certificate = keyStore.getCertificate(keyAlias) as? X509Certificate
                ?: error("NovaRun TLS certificate is unavailable")
            certificate.checkValidity()

            val keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
            )
            keyManagerFactory.init(keyStore, null)

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(keyManagerFactory.keyManagers, null, SecureRandom())
            }

            val fingerprint = MessageDigest.getInstance("SHA-256")
                .digest(certificate.encoded)
                .joinToString(":") { byte -> "%02X".format(byte) }

            // Android Keystore TLS signing is verified with RSA PKCS#1 on
            // TLS 1.2. Keep the LAN endpoint on TLS 1.2 until the API 35+
            // RSA-PSS/MGF1 path has independent device verification.
            val protocols = listOf("TLSv1.2")

            // Remove the obsolete key only after the v2 identity and SSLContext
            // are ready. A failed migration therefore leaves the old entry intact.
            if (keyStore.containsAlias(legacyAlias)) {
                runCatching { keyStore.deleteEntry(legacyAlias) }
            }

            return LanTlsIdentity(sslContext, fingerprint, protocols)
        }
    }
}
