package com.keyvault.app

import com.keyvault.app.data.Vault
import javax.crypto.SecretKey

object Session {
    var key: SecretKey? = null
    var vault: Vault? = null
    fun clear() { key = null; vault = null }
}