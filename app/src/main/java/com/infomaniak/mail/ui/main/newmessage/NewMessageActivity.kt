/*
 * Infomaniak kMail - Android
 * Copyright (C) 2022 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.infomaniak.mail.ui.main.newmessage

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.infomaniak.mail.R
import com.infomaniak.mail.databinding.ActivityNewMessageBinding

class NewMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewMessageBinding

    private var isKeyboardShowing: Boolean = false

    private var isBoldActivated = false
    private var isItalicActivated = false
    private var isUnderlineActivated = false
    private var isTextColorActivated = false
    private var isBackgroundColorActivated = false

    private val defaultTextColor = Color.WHITE
    private val modifiedTextColor = Color.CYAN
    private val defaultBackgroundColor = Color.BLACK
    private val modifiedBackgroundColor = Color.RED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMessageBinding.inflate(layoutInflater).also { setContentView(it.root) }

        setKeyboardVisibilityListener()
        configureRichEditor()
        configureRichEditorButtons()
    }

    private fun setKeyboardVisibilityListener() = with(binding) {
        root.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            root.getWindowVisibleDisplayFrame(r)
            val screenHeight = root.rootView.height

            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                // keyboard is opened
                if (!isKeyboardShowing) {
                    isKeyboardShowing = true
                    onKeyboardVisibilityChanged(true)
                }
            } else {
                // keyboard is closed
                if (isKeyboardShowing) {
                    isKeyboardShowing = false
                    onKeyboardVisibilityChanged(false)
                }
            }
        }
    }

    private fun onKeyboardVisibilityChanged(isOpen: Boolean) {
        binding.formattingToolbar.isVisible = isOpen
    }

    private fun configureRichEditor() = with(binding.richEditor) {
        setEditorHeight(200)
        setEditorFontSize(22)
        setEditorFontColor(defaultTextColor)
        setTextColor(defaultTextColor)
        setEditorBackgroundColor(defaultBackgroundColor)
        setBackgroundColor(defaultBackgroundColor)
        setPadding(10, 10, 10, 10)
        setPlaceholder("Insert text here...")
        setOnTextChangeListener { binding.richEditorPreview.text = it }
    }

    private fun configureRichEditorButtons() = with(binding) {
        boldButton.setOnClickListener { toggleBold() }
        italicButton.setOnClickListener { toggleItalic() }
        underlineButton.setOnClickListener { toggleUnderline() }
        textColorButton.setOnClickListener { toggleTextColor() }
        backgroundColorButton.setOnClickListener { toggleBackgroundColor() }
        clearFormattingButton.setOnClickListener { disableFormatting() }
    }

    private fun toggleBold() {
        isBoldActivated = !isBoldActivated
        val resId = if (isBoldActivated) R.drawable.ic_format_bold_activated else R.drawable.ic_format_bold
        with(binding) {
            richEditor.setBold()
            boldButton.setImageResource(resId)
        }
    }

    private fun toggleItalic() {
        isItalicActivated = !isItalicActivated
        val resId = if (isItalicActivated) R.drawable.ic_format_italic_activated else R.drawable.ic_format_italic
        with(binding) {
            richEditor.setItalic()
            italicButton.setImageResource(resId)
        }
    }

    private fun toggleUnderline() {
        isUnderlineActivated = !isUnderlineActivated
        val resId = if (isUnderlineActivated) R.drawable.ic_format_underline_activated else R.drawable.ic_format_underline
        with(binding) {
            richEditor.setUnderline()
            underlineButton.setImageResource(resId)
        }
    }

    private fun toggleTextColor() {
        isTextColorActivated = !isTextColorActivated
        val (resId, textColor) = if (isTextColorActivated) {
            R.drawable.ic_format_text_color_activated to modifiedTextColor
        } else {
            R.drawable.ic_format_text_color to defaultTextColor
        }
        with(binding) {
            richEditor.setTextColor(textColor)
            textColorButton.setImageResource(resId)
        }
    }

    private fun toggleBackgroundColor() {
        isBackgroundColorActivated = !isBackgroundColorActivated
        val (resId, backgroundColor) = if (isBackgroundColorActivated) {
            R.drawable.ic_format_background_color_activated to modifiedBackgroundColor
        } else {
            R.drawable.ic_format_background_color to defaultBackgroundColor
        }
        with(binding) {
            richEditor.setTextBackgroundColor(backgroundColor)
            backgroundColorButton.setImageResource(resId)
        }
    }

    private fun disableFormatting() {
        // if (isBoldActivated) toggleBold()
        // if (isItalicActivated) toggleItalic()
        // if (isUnderlineActivated) toggleUnderline()
        // if (isTextColorActivated) toggleTextColor()
        // if (isBackgroundColorActivated) toggleBackgroundColor()
        binding.richEditor.removeFormat()
    }
}
