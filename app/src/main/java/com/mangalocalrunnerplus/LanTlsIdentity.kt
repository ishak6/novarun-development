package com.mangalocalrunnerplus

import android.content.Context
import android.os.Build
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
                val paddings = if (Build.VERSION.SDK_INT >= 35) {
                    arrayOf(
                        KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                        KeyProperties.SIGNATURE_PADDING_RSA_PSS
                    )
                } else {
                    arrayOf(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                }
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
                    .apply {
                        // MGF1 digest selection became configurable in API 35.
                        // TLS 1.3 RSA-PSS uses SHA-256; older Android versions
                        // stay on TLS 1.2 with RSA PKCS#1 signatures.
                        if (Build.VERSION.SDK_INT >= 35) {
                            setMgf1Digests(KeyProperties.DIGEST_SHA256)
                        }
                    }
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

            // TLS 1.3 RSA authentication requires RSA-PSS/MGF1 support. API 35
            // can authorize SHA-256 MGF1; earlier Android releases use TLS 1.2.
            val protocols = if (Build.VERSION.SDK_INT >= 35) {
                listOf("TLSv1.3", "TLSv1.2")
            } else {
                listOf("TLSv1.2")
            }

            // Remove the obsolete key only after the v2 identity and SSLContext
            // are ready. A failed migration therefore leaves the old entry intact.
            if (keyStore.containsAlias(legacyAlias)) {
                runCatching { keyStore.deleteEntry(legacyAlias) }
            }

            return LanTlsIdentity(sslContext, fingerprint, protocols)
        }
    }
}
