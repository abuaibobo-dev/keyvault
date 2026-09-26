package com.keyvault.app.ui

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.keyvault.app.security.BiometricVault
import com.keyvault.app.ui.components.SensitiveClipboard
import com.keyvault.app.ui.theme.KeyVaultTheme
import com.keyvault.app.ui.theme.VaultBackground
import com.keyvault.app.util.LockPolicy

class MainActivity : FragmentActivity() {
    private lateinit var model: AppViewModel
    private lateinit var biometric: BiometricVault
    private var themeMode by mutableStateOf("dark")
    private var accentIndex by mutableIntStateOf(0)
    private var stoppedAt = 0L
    private var externalOperation = false
    private var pendingExport: ByteArray? = null
    private var pendingImport by mutableStateOf<ByteArray?>(null)
    private var biometricConfigured by mutableStateOf(false)

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        externalOperation = false
        val bytes = pendingExport
        pendingExport = null
        if (uri != null && bytes != null) {
            try { contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
            catch (_: Exception) { model.reportError("导出失败") }
        }
    }
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        externalOperation = false
        if (uri != null) {
            try { pendingImport = contentResolver.openInputStream(uri)?.use { it.readBytes() } }
            catch (_: Exception) { model.reportError("无法读取备份文件") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[AppViewModel::class.java]
        biometric = BiometricVault(this)
        biometricConfigured = biometric.available() && biometric.hasCredential()
        themeMode = model.settings.theme
        accentIndex = model.settings.accent
        setContent {
            KeyVaultTheme(themeMode, accentIndex) {
                VaultBackground(themeMode) {
                    AppRoot(
                        model = model,
                        biometricAvailable = biometricConfigured,
                        biometricHardwareAvailable = biometric.available(),
                        onBiometricUnlock = {
                            externalOperation = true
                            biometric.unlock { password ->
                                externalOperation = false
                                if (password == null) model.reportError("生物识别未完成，请输入主密码")
                                else model.unlock(password)
                            }
                        },
                        onEnableBiometric = { password, done ->
                            externalOperation = true
                            biometric.enroll(password) { success ->
                                externalOperation = false
                                biometricConfigured = success
                                done(success)
                            }
                        },
                        onDisableBiometric = { biometric.clear(); biometricConfigured = false },
                        onExport = {
                            try {
                                pendingExport = model.exportBytes()
                                externalOperation = true
                                createDocument.launch("KeyVault.kvault")
                            } catch (_: Exception) { model.reportError("导出失败") }
                        },
                        onImport = { externalOperation = true; openDocument.launch(arrayOf("*/*")) },
                        onTheme = { themeMode = it; model.settings.theme = it },
                        onAccent = { accentIndex = it; model.settings.accent = it },
                        themeMode = themeMode,
                        accentIndex = accentIndex,
                    )
                    pendingImport?.let { bytes ->
                        ImportDialog(
                            onDismiss = { pendingImport = null },
                            onImport = { password, merge ->
                                model.importBytes(bytes, password, merge) { if (it) pendingImport = null }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!externalOperation && !isChangingConfigurations) {
            stoppedAt = SystemClock.elapsedRealtime()
            if (model.settings.lockMinutes == 0) { model.lock(); pendingImport = null }
        }
    }

    override fun onStart() {
        super.onStart()
        SensitiveClipboard.clearExpired(this)
        if (!externalOperation && LockPolicy.shouldLock(stoppedAt, SystemClock.elapsedRealtime(), model.settings.lockMinutes)) {
            model.lock()
            pendingImport = null
        }
        stoppedAt = 0L
    }
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (CharArray, Boolean) -> Unit) {
    var password by remember { mutableStateOf("") }
    var merge by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入加密备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("输入该文件的主密码，然后选择如何处理现有数据。")
                SecretField(password, { password = it }, "备份主密码")
                Row {
                    FilterChip(merge, { merge = true }, label = { Text("合并") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(!merge, { merge = false }, label = { Text("覆盖") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (password.isNotEmpty()) { onImport(password.toCharArray(), merge); password = "" } }) { Text("导入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
