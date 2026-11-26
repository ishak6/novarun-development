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
import java.security.spec.ECGenParameterSpec
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.security.auth.x500.X500Principal

/**
 * Creates and retains a device-local P-256 ECDSA TLS identity in Android Keystore.
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
        private const val VALIDITY_DAYS = 3650L

        fun create(context: Context): LanTlsIdentity {
            // v3 migrates from RSA to P-256 ECDSA. Conscrypt may request an RSA
            // padding mode that the old Android Keystore key did not authorize,
            // causing TLS handshakes to fail on supported Android versions.
            val keyAlias = "novarun_${context.packageName}_lan_tls_identity_v3"
            val legacyAliases = listOf(
                "novarun_${context.packageName}_lan_tls_identity_v1",
                "novarun_${context.packageName}_lan_tls_identity_v2"
            )
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
            }

            if (!keyStore.containsAlias(keyAlias)) {
                val now = System.currentTimeMillis()
                val generator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    KEYSTORE_PROVIDER
                )
                val spec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
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
            check(certificate.publicKey.algorithm.equals("EC", ignoreCase = true)) {
                "NovaRun TLS identity must use an EC public key"
            }

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

            // Retire legacy RSA keys only after the new identity and SSLContext
            // are usable. A failed migration leaves the previous identity intact.
            legacyAliases.forEach { alias ->
                if (keyStore.containsAlias(alias)) {
                    runCatching { keyStore.deleteEntry(alias) }
                }
            }

            return LanTlsIdentity(sslContext, fingerprint)
        }
    }
}
