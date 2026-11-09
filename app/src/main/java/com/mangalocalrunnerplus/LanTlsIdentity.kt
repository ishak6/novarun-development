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
    val sha256Fingerprint: String
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "novarun_lan_tls_identity_v1"
        private const val VALIDITY_DAYS = 3650L

        fun create(context: Context): LanTlsIdentity {
            // Access context deliberately: the identity belongs to this app/device.
            context.applicationContext
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
            }

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val now = System.currentTimeMillis()
                val generator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    KEYSTORE_PROVIDER
                )
                generator.initialize(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    )
                        .setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setCertificateSubject(X500Principal("CN=NovaRun Local Server"))
                        .setCertificateSerialNumber(BigInteger.valueOf(now.coerceAtLeast(1L)))
                        .setCertificateNotBefore(Date(now - 60_000L))
                        .setCertificateNotAfter(Date(now + VALIDITY_DAYS * 24L * 60L * 60L * 1000L))
                        .build()
                )
                generator.generateKeyPair()
            }

            val certificate = keyStore.getCertificate(KEY_ALIAS) as? X509Certificate
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

            return LanTlsIdentity(sslContext, fingerprint)
        }
    }
}
