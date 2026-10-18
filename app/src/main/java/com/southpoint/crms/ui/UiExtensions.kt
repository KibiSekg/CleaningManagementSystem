package com.southpoint.crms.ui

import android.util.Patterns
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "2026-10-21" -> "21 Oct 2026" (same format the original views used). */
fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
} catch (e: Exception) {
    iso
}

/** The text typed into the field inside this TextInputLayout (not trimmed, so passwords stay exact). */
fun TextInputLayout.value(): String = editText?.text?.toString().orEmpty()

/** Removes the red error as soon as the user edits the field again. */
fun TextInputLayout.clearErrorWhenTyped() {
    editText?.doOnTextChanged { _, _, _, _ -> error = null }
}

fun TextInputLayout.requireValue(message: String = "This field is required"): Boolean {
    val ok = value().isNotBlank()
    error = if (ok) null else message
    return ok
}

fun TextInputLayout.requireEmail(): Boolean {
    val email = value().trim()
    error = when {
        email.isEmpty() -> "Email is required"
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address"
        else -> null
    }
    return error == null
}

fun TextInputLayout.requirePassword(): Boolean {
    val ok = value().isNotEmpty()
    error = if (ok) null else "Password is required"
    return ok
}

/** For registration / changing a password: at least 6 characters (Firebase minimum). */
fun TextInputLayout.requireNewPassword(): Boolean {
    val password = value()
    error = when {
        password.isEmpty() -> "Password is required"
        password.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }
    return error == null
}

fun TextInputLayout.requireMatch(other: TextInputLayout): Boolean {
    val ok = value() == other.value()
    error = if (ok) null else "Passwords do not match"
    return ok
}

/** Fills an exposed-dropdown field with a list of choices. */
fun MaterialAutoCompleteTextView.setOptions(options: List<String>) {
    setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, options))
}
