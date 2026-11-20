package com.vongquay.luckywheel

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var wheel: WheelView
    private lateinit var spinButton: MaterialButton
    private lateinit var resultText: TextView

    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        wheel = findViewById(R.id.wheel)
        spinButton = findViewById(R.id.spinButton)
        resultText = findViewById(R.id.resultText)

        wheel.items = loadItems()
        wheel.onSegmentTick = { wheel.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
        wheel.onSpinEnd = ::showResult

        spinButton.setOnClickListener { startSpin() }
        wheel.setOnClickListener { startSpin() }
    }

    private fun startSpin() {
        if (wheel.isSpinning) return
        spinButton.isEnabled = false
        resultText.setText(R.string.spinning)
        wheel.spin()
    }

    private fun showResult(index: Int) {
        spinButton.isEnabled = true
        val item = wheel.items.getOrNull(index) ?: return
        resultText.text = getString(R.string.result_line, item)
        wheel.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.result_title)
            .setMessage(item)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton(R.string.remove_item) { _, _ -> removeItem(index) }
            .show()
    }

    private fun removeItem(index: Int) {
        val items = wheel.items
        if (items.size <= MIN_ITEMS) {
            Toast.makeText(this, R.string.need_two, Toast.LENGTH_SHORT).show()
            return
        }
        val updated = items.toMutableList().apply { removeAt(index) }
        saveItems(updated)
        resultText.setText(R.string.hint)
    }

    // ---- Items -------------------------------------------------------------

    private fun defaultItems() = resources.getStringArray(R.array.default_items).toList()

    /** User-edited items, or the default list in the current language. */
    private fun loadItems(): List<String> =
        prefs.getString(KEY_ITEMS, null)
            ?.split('\n')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.size >= MIN_ITEMS }
            ?: defaultItems()

    private fun saveItems(items: List<String>) {
        prefs.edit { putString(KEY_ITEMS, items.joinToString("\n")) }
        wheel.items = items
    }

    private fun showEditDialog() {
        if (wheel.isSpinning) return
        val view = layoutInflater.inflate(R.layout.dialog_edit_items, null)
        val input = view.findViewById<TextInputEditText>(R.id.itemsInput)
        input.setText(wheel.items.joinToString("\n"))

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_items)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .show()

        // Validate before closing.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val items = input.text.toString()
                .split('\n')
                .map { it.trim().take(MAX_ITEM_LENGTH) }
                .filter { it.isNotEmpty() }
            when {
                items.size < MIN_ITEMS ->
                    Toast.makeText(this, R.string.need_two, Toast.LENGTH_SHORT).show()
                items.size > MAX_ITEMS ->
                    Toast.makeText(this, getString(R.string.too_many, MAX_ITEMS), Toast.LENGTH_SHORT).show()
                else -> {
                    saveItems(items)
                    resultText.setText(R.string.hint)
                    dialog.dismiss()
                }
            }
        }
    }

    private fun resetItems() {
        if (wheel.isSpinning) return
        prefs.edit { remove(KEY_ITEMS) }
        wheel.items = defaultItems()
        resultText.setText(R.string.hint)
    }

    // ---- Menu / language ---------------------------------------------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val current = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: resources.configuration.locales[0].language
        menu.findItem(if (current == "bn") R.id.lang_bn else R.id.lang_en)?.isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_edit -> { showEditDialog(); true }
        R.id.action_reset -> { resetItems(); true }
        R.id.lang_en -> { setLanguage("en"); true }
        R.id.lang_bn -> { setLanguage("bn"); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    companion object {
        private const val PREFS = "lucky_wheel"
        private const val KEY_ITEMS = "items"
        private const val MIN_ITEMS = 2
        private const val MAX_ITEMS = 24
        private const val MAX_ITEM_LENGTH = 40
    }
}
