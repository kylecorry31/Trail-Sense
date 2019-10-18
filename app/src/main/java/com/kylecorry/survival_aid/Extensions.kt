package com.kylecorry.survival_aid

import android.app.Activity
import android.content.SharedPreferences
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

inline fun FragmentManager.doTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

inline fun Activity.editPrefs(name: String, mode: Int, func: SharedPreferences.Editor.() -> Unit) {
    val prefs = getSharedPreferences(name, mode)
    val editor = prefs.edit()
    editor.func()
    editor.apply()
}