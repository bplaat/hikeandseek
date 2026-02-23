/*
 * Copyright (c) 2026 Bastiaan van der Plaat
 *
 * SPDX-License-Identifier: MIT
 */

package nl.bplaat.hikeandseek

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "hikeandseek_prefs"
    private const val KEY_RDX_PREFIX = "rdx_prefix"
    private const val KEY_RDX_SUFFIX = "rdx_suffix"
    private const val KEY_RDY_PREFIX = "rdy_prefix"
    private const val KEY_RDY_SUFFIX = "rdy_suffix"

    private const val DEFAULT_OFFSET = 0

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getRdxPrefix(): Int = prefs.getString(KEY_RDX_PREFIX, DEFAULT_OFFSET.toString())?.toIntOrNull() ?: DEFAULT_OFFSET
    fun getRdxSuffix(): Int = prefs.getString(KEY_RDX_SUFFIX, DEFAULT_OFFSET.toString())?.toIntOrNull() ?: DEFAULT_OFFSET
    fun getRdyPrefix(): Int = prefs.getString(KEY_RDY_PREFIX, DEFAULT_OFFSET.toString())?.toIntOrNull() ?: DEFAULT_OFFSET
    fun getRdySuffix(): Int = prefs.getString(KEY_RDY_SUFFIX, DEFAULT_OFFSET.toString())?.toIntOrNull() ?: DEFAULT_OFFSET

    fun setOffsets(rdxPrefix: Int, rdxSuffix: Int, rdyPrefix: Int, rdySuffix: Int) {
        prefs.edit().apply {
            putString(KEY_RDX_PREFIX, rdxPrefix.toString())
            putString(KEY_RDX_SUFFIX, rdxSuffix.toString())
            putString(KEY_RDY_PREFIX, rdyPrefix.toString())
            putString(KEY_RDY_SUFFIX, rdySuffix.toString())
            apply()
        }
    }
}
