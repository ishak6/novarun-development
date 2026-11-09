package com.mangalocalrunnerplus

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class LanCredentials(val username: String, val password: String)

/** Encrypts LAN credentials with an AES-256 key held by Android Keystore. */
object LanCredentialsStore {
    private const val PREFS = "novarun_lan_credentials"
    private const val KEY_ALIAS = "novarun_lan_credentials_aes"
    private const val IV_KEY = "iv"
    private const val DATA_KEY = "ciphertext"

    @Synchronized
    fun get(context: Context): LanCredentials {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val encodedIv = prefs.getString(IV_KEY, null)
        val encodedData = prefs.getString(DATA_KEY, null)
        if (encodedIv != null && encodedData != null) {
            runCatching {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP)))
                val plain = cipher.doFinal(Base64.decode(encodedData, Base64.NO_WRAP)).toString(StandardCharsets.UTF_8)
                val split = plain.indexOf('\n')
                require(split > 0)
                return LanCredentials(plain.substring(0, split), plain.substring(split + 1))
            }
        }
        val generated = ByteArray(24).also { java.security.SecureRandom().nextBytes(it) }
        val password = Base64.encodeToString(generated, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val defaults = LanCredentials("novarun", password)
        save(context, defaults.username, defaults.password)
        return defaults
    }

    @Synchronized
    fun save(context: Context, username: String, password: String) {
        require(username.isNotBlank() && username.length <= 64 && !username.contains('\n') && !username.contains(':')) { "Username must be 1–64 characters and cannot contain ':'." }
        require(password.length >= 8 && password.length <= 128 && !password.contains('\n')) { "Password must be 8–128 characters." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal("$username\n$password".toByteArray(StandardCharsets.UTF_8))
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(DATA_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .commit()
        check(saved) { "Could not save encrypted LAN credentials." }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        return generator.generateKey()
    }
}

@Composable
fun LanCredentialsSettingsCard() {
    val context = LocalContext.current
    val initial = remember(context) { LanCredentialsStore.get(context) }
    var username by remember { mutableStateOf(initial.username) }
    var password by remember { mutableStateOf(initial.password) }
    var visible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Credentials are encrypted on this device.") }

    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CUSTOM LAN LOGIN")
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                singleLine = true
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password (8–128 characters)") },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
            )
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide password" else "Show password") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NovaRun username", username))
                    message = "Username copied."
                }) { Text("Copy username") }
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NovaRun password", password))
                    message = "Password copied."
                }) { Text("Copy password") }
            }
            Button(
                onClick = {
                    runCatching { LanCredentialsStore.save(context, username.trim(), password) }
                        .onSuccess { message = "Username and password saved securely." }
                        .onFailure { message = it.message ?: "Could not save credentials." }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Login Credentials") }
            Text(message)
            Text("Username cannot contain ':' or line breaks. Password requires at least 8 characters.")
        }
    }
}
