/*
 * Copyright (C) 2018, Jorge Cloquell Ribera, Nadine Kost
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jcloquell.androidsecurestorage

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

@SuppressLint("CommitPrefEdits")
internal class CipherHelper constructor(private val sharedPreferences: SharedPreferences,
    private val isAsynchronous: Boolean) {

  companion object {
    private const val TRANSFORMATION_ASYMMETRIC = "RSA/ECB/PKCS1Padding"
    private const val TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"
    private const val AES_ALGORITHM = "AES"
    private const val IV_PREFERENCES_KEY = "IvPreferencesKey"
  }

  private val asymmetricCipher = Cipher.getInstance(TRANSFORMATION_ASYMMETRIC)
  private val symmetricCipher = Cipher.getInstance(TRANSFORMATION_SYMMETRIC)

  internal fun encrypt(sharedPreferencesKey: String, textToEncrypt: String, key: Key?): String {
    symmetricCipher.init(Cipher.ENCRYPT_MODE, key)
    val bytes = symmetricCipher.doFinal(textToEncrypt.toByteArray())
    saveIvInSharedPreferences(sharedPreferencesKey, symmetricCipher.iv)
    return Base64.encodeToString(bytes, Base64.DEFAULT)
  }

  internal fun decrypt(sharedPreferencesKey: String, textToDecrypt: String, key: Key?): String {
    symmetricCipher.init(Cipher.DECRYPT_MODE, key,
        IvParameterSpec(getIvFromSharedPreferences(sharedPreferencesKey)))
    val encryptedData = Base64.decode(textToDecrypt, Base64.DEFAULT)
    val decodedData = symmetricCipher.doFinal(encryptedData)
    return String(decodedData)
  }

  internal fun wrapKey(keyToBeWrapped: Key?, keyToWrapWith: Key?): String {
    asymmetricCipher.init(Cipher.WRAP_MODE, keyToWrapWith)
    val bytes = asymmetricCipher.wrap(keyToBeWrapped)
    return Base64.encodeToString(bytes, Base64.DEFAULT)
  }

  internal fun unwrapKey(wrappedKeyData: String?, keyToUnwrapWith: Key?): Key {
    asymmetricCipher.init(Cipher.UNWRAP_MODE, keyToUnwrapWith)
    val encryptedData = Base64.decode(wrappedKeyData, Base64.DEFAULT)
    return asymmetricCipher.unwrap(encryptedData, AES_ALGORITHM, Cipher.SECRET_KEY)
  }

  private fun saveIvInSharedPreferences(sharedPreferencesKey: String, iv: ByteArray) {
    val encodedIv = Base64.encodeToString(iv, Base64.DEFAULT)
    sharedPreferences.edit().putString("$IV_PREFERENCES_KEY$sharedPreferencesKey", encodedIv)
        .save(isAsynchronous)
  }

  private fun getIvFromSharedPreferences(sharedPreferencesKey: String): ByteArray {
    val encodedIv = sharedPreferences.getString("$IV_PREFERENCES_KEY$sharedPreferencesKey", null)
    return Base64.decode(encodedIv, Base64.DEFAULT)
  }
}