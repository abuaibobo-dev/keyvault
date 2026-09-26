package com.keyvault.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyvault.app.Session
import com.keyvault.app.data.KeyItem
import com.keyvault.app.data.NoteItem
import com.keyvault.app.data.Vault
import com.keyvault.app.data.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = VaultRepository(application.filesDir)
    val settings = SettingsStore(application)
    private val _vault = MutableStateFlow<Vault?>(null)
    val vault = _vault.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow("")
    val error = _error.asStateFlow()
    val firstRun: Boolean get() = !repo.hasAnyData()
    private var key: SecretKey? = null
    private var generation = 0
    private var failedUnlocks = 0
    private var retryAfter = 0L

    fun clearError() { _error.value = "" }
    fun reportError(message: String) { _error.value = message }

    fun unlock(password: CharArray, confirm: CharArray? = null, onSuccess: (() -> Unit)? = null) {
        if (_busy.value) { password.fill('\u0000'); confirm?.fill('\u0000'); return }
        if (SystemClock.elapsedRealtime() < retryAfter) {
            _error.value = "尝试过多，请稍后再试"
            password.fill('\u0000'); confirm?.fill('\u0000'); return
        }
        if (firstRun && (password.size < 8 || confirm == null || !password.contentEquals(confirm))) {
            _error.value = if (password.size < 8) "主密码至少 8 位" else "两次密码不一致"
            password.fill('\u0000'); confirm?.fill('\u0000'); return
        }
        _busy.value = true
        val startedGeneration = generation
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        if (firstRun) {
                            val k = repo.initVault(password)
                            k to repo.load(k)
                        } else repo.unlock(password)
                    } finally { password.fill('\u0000'); confirm?.fill('\u0000') }
                }
                if (startedGeneration != generation) return@launch
                key = result.first
                Session.key = result.first
                Session.vault = result.second
                _vault.value = result.second
                failedUnlocks = 0
                retryAfter = 0L
                _error.value = ""
                onSuccess?.invoke()
            } catch (_: Exception) {
                failedUnlocks++
                if (failedUnlocks >= 5) {
                    retryAfter = SystemClock.elapsedRealtime() + 30_000L
                    failedUnlocks = 0
                    _error.value = "尝试过多，请 30 秒后重试"
                } else _error.value = "解锁失败，请检查密码或文件"
            }
            finally { _busy.value = false }
        }
    }

    fun verifyPassword(password: CharArray, onResult: (Boolean) -> Unit) {
        val startedGeneration = generation
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repo.unlock(password) }
                onResult(startedGeneration == generation)
            } catch (_: Exception) { _error.value = "主密码不正确"; onResult(false) }
            finally { password.fill('\u0000') }
        }
    }

    fun lock() {
        generation++
        key = null
        Session.clear()
        _vault.value = null
        _error.value = ""
    }

    private fun update(change: (Vault) -> Unit) {
        val current = _vault.value ?: return
        val secret = key ?: return
        val next = Vault(current.keys.toMutableList(), current.notes.toMutableList())
        change(next)
        try {
            repo.save(secret, next)
            Session.vault = next
            _vault.value = next
            _error.value = ""
        } catch (_: Exception) { _error.value = "保存失败，请重试" }
    }

    fun saveKey(item: KeyItem) = update { v ->
        val index = v.keys.indexOfFirst { it.id == item.id }
        if (index < 0) v.keys.add(item) else v.keys[index] = item
    }
    fun deleteKey(id: String) = update { it.keys.removeAll { k -> k.id == id } }
    fun saveNote(item: NoteItem) = update { v ->
        val index = v.notes.indexOfFirst { it.id == item.id }
        if (index < 0) v.notes.add(item) else v.notes[index] = item
    }
    fun deleteNote(id: String) = update { it.notes.removeAll { n -> n.id == id } }
    fun toggleKeyPin(item: KeyItem) = saveKey(item.copy(pinned = !item.pinned, updatedAt = System.currentTimeMillis()))
    fun toggleNotePin(item: NoteItem) = saveNote(item.copy(pinned = !item.pinned, updatedAt = System.currentTimeMillis()))
    fun toggleFavorite(item: NoteItem) = saveNote(item.copy(favorite = !item.favorite, updatedAt = System.currentTimeMillis()))

    fun exportBytes(): ByteArray = repo.exportBytes()
    fun importBytes(bytes: ByteArray, password: CharArray, merge: Boolean, onDone: (Boolean) -> Unit) {
        val secret = key ?: run { password.fill('\u0000'); onDone(false); return }
        val startedGeneration = generation
        viewModelScope.launch {
            try {
                val imported = withContext(Dispatchers.IO) {
                    try { repo.importBytes(bytes, password) } finally { password.fill('\u0000') }
                }
                if (startedGeneration != generation) { onDone(false); return@launch }
                val current = _vault.value ?: return@launch
                val next = if (!merge) imported else Vault(
                    (current.keys + imported.keys).associateBy { it.id }.values.toMutableList(),
                    (current.notes + imported.notes).associateBy { it.id }.values.toMutableList(),
                )
                withContext(Dispatchers.IO) { repo.save(secret, next) }
                if (startedGeneration != generation) { onDone(false); return@launch }
                Session.vault = next
                _vault.value = next
                _error.value = ""
                onDone(true)
            } catch (_: Exception) { _error.value = "导入失败，请检查文件和密码"; onDone(false) }
        }
    }
}
