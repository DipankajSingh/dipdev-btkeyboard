package com.dipdev.btkeyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

/**
 * Custom canvas-drawn QWERTY keyboard view (M3 palette).
 * Supports: Normal (lowercase), Shifted (uppercase), Number/Symbol mode, Caps Lock.
 * Calls [onKeyEvent] whenever a key is pressed.
 */
class KeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── Callback ──────────────────────────────────────────────────────
    var onKeyEvent: ((KeyEvent) -> Unit)? = null

    sealed class KeyEvent {
        data class Char(val c: kotlin.Char) : KeyEvent()
        data class Special(val code: Byte) : KeyEvent()
        object Backspace : KeyEvent()
        object Enter : KeyEvent()
        object Space : KeyEvent()
    }

    // ── State ─────────────────────────────────────────────────────────
    enum class InputMode { NORMAL, SHIFTED, CAPS, NUMBERS }

    private var mode = InputMode.NORMAL

    // ── Key definitions ───────────────────────────────────────────────
    private data class Key(
        val label: String,
        val shiftLabel: String = label,
        val numLabel: String = label,
        val type: Type = Type.CHAR,
        val widthFactor: Float = 1f,
        val code: Byte = 0
    ) {
        enum class Type { CHAR, BACKSPACE, ENTER, SPACE, SHIFT, NUMBER_TOGGLE }
    }

    private val rows: List<List<Key>> = listOf(
        listOf(
            Key("q", "Q", "1"), Key("w", "W", "2"), Key("e", "E", "3"), Key("r", "R", "4"),
            Key("t", "T", "5"), Key("y", "Y", "6"), Key("u", "U", "7"), Key("i", "I", "8"),
            Key("o", "O", "9"), Key("p", "P", "0")
        ),
        listOf(
            Key("a", "A", "@"), Key("s", "S", "#"), Key("d", "D", "$"), Key("f", "F", "%"),
            Key("g", "G", "&"), Key("h", "H", "-"), Key("j", "J", "+"), Key("k", "K", "("),
            Key("l", "L", ")")
        ),
        listOf(
            Key("⇧", "⇧", "⇧", type = Key.Type.SHIFT, widthFactor = 1.4f),
            Key("z", "Z", "!"), Key("x", "X", "\""), Key("c", "C", "'"),
            Key("v", "V", ":"), Key("b", "B", ";"), Key("n", "N", "/"),
            Key("m", "M", "?"),
            Key("⌫", "⌫", "⌫", type = Key.Type.BACKSPACE, widthFactor = 1.4f)
        ),
        listOf(
            Key("123", "123", "ABC", type = Key.Type.NUMBER_TOGGLE, widthFactor = 1.5f),
            Key(","),
            Key(
                "   SPACE   ",
                "   SPACE   ",
                "   SPACE   ",
                type = Key.Type.SPACE,
                widthFactor = 5f
            ),
            Key("."),
            Key("↵", "↵", "↵", type = Key.Type.ENTER, widthFactor = 1.5f)
        )
    )

    // ── M3 Palette colors ─────────────────────────────────────────────
    private val colorBackground = Color.parseColor("#080A18")   // NavyDeep
    private val colorKeyNormal = Color.parseColor("#1C1E3A")   // NavyKey
    private val colorKeySpecial = Color.parseColor("#111327")   // NavySurface
    private val colorKeyPressed = Color.parseColor("#00D4FF")   // CyanPrimary
    private val colorKeyShiftOn = Color.parseColor("#00D4FF")   // CyanPrimary
    private val colorKeyCaps = Color.parseColor("#7B52FF")   // purple for caps
    private val colorKeyNumActive = Color.parseColor("#00D4FF")   // CyanPrimary
    private val colorTextNormal = Color.WHITE
    private val colorTextPressed = Color.parseColor("#080A18")   // NavyDeep
    private val colorTextSpecial = Color.parseColor("#B0BAD0")
    private val colorBorder = Color.parseColor("#252548")   // NavyBorder

    // ── Paint objects ─────────────────────────────────────────────────
    private val density get() = context.resources.displayMetrics.density
    private val keyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = colorBorder
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextNormal
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val cornerRadiusDp = 10f
    private val keyMarginDp = 4f

    private data class KeyRect(val rect: RectF, val row: Int, val col: Int)

    private val keyRects = mutableListOf<KeyRect>()
    private var pressedKey: Pair<Int, Int>? = null

    // ── Measure ───────────────────────────────────────────────────────
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val keyMargin = keyMarginDp * density
        val keyH = (w / 10f * 1.5f).coerceAtLeast(90f)
        val naturalH = ((keyH + keyMargin * 2) * rows.size + keyMargin * 2).toInt()
        val h = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> naturalH.coerceAtMost(heightSize)
            else -> naturalH
        }
        setMeasuredDimension(w, h)
    }

    // ── Draw ──────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(colorBackground)
        keyRects.clear()

        val keyMargin = keyMarginDp * density
        val cornerRadius = cornerRadiusDp * density
        val rows = rows
        val keyHeight = (height - keyMargin * (rows.size + 1)) / rows.size

        textPaint.textSize = keyHeight * 0.40f

        var y = keyMargin
        rows.forEachIndexed { rIdx, row ->
            val totalFactor = row.sumOf { it.widthFactor.toDouble() }.toFloat()
            val unitWidth = (width - keyMargin * (row.size + 1)) / totalFactor
            var x = keyMargin

            row.forEachIndexed { cIdx, key ->
                val kw = unitWidth * key.widthFactor
                val rect = RectF(x, y, x + kw, y + keyHeight)
                keyRects.add(KeyRect(rect, rIdx, cIdx))

                val isPressed = pressedKey == Pair(rIdx, cIdx)
                val isSpecial = key.type != Key.Type.CHAR

                keyBgPaint.color = when {
                    isPressed -> colorKeyPressed
                    mode == InputMode.SHIFTED && key.type == Key.Type.SHIFT -> colorKeyShiftOn
                    mode == InputMode.CAPS && key.type == Key.Type.SHIFT -> colorKeyCaps
                    mode == InputMode.NUMBERS && key.type == Key.Type.NUMBER_TOGGLE -> colorKeyNumActive
                    isSpecial -> colorKeySpecial
                    else -> colorKeyNormal
                }

                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)

                // Draw border
                borderPaint.alpha = if (isPressed) 0 else 80
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

                val displayLabel = when (mode) {
                    InputMode.NORMAL, InputMode.CAPS -> key.label
                    InputMode.SHIFTED -> key.shiftLabel
                    InputMode.NUMBERS -> key.numLabel
                }
                val label = if (mode == InputMode.CAPS && key.type == Key.Type.CHAR)
                    displayLabel.uppercase() else displayLabel

                textPaint.color = when {
                    isPressed -> colorTextPressed
                    isSpecial -> colorTextSpecial
                    else -> colorTextNormal
                }

                canvas.drawText(
                    label,
                    rect.centerX(),
                    rect.centerY() + textPaint.textSize * 0.36f,
                    textPaint
                )
                x += kw + keyMargin
            }
            y += keyHeight + keyMargin
        }
    }

    // ── Touch ─────────────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val hit = keyRects.find { it.rect.contains(event.x, event.y) }
                if (hit != null && pressedKey != Pair(hit.row, hit.col)) {
                    pressedKey = Pair(hit.row, hit.col)
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                val hit = keyRects.find { it.rect.contains(event.x, event.y) }
                if (hit != null) fireKey(hit.row, hit.col)
                pressedKey = null
                invalidate()
            }
        }
        return true
    }

    private fun fireKey(row: Int, col: Int) {
        val key = rows[row][col]
        when (key.type) {
            Key.Type.BACKSPACE -> onKeyEvent?.invoke(KeyEvent.Backspace)
            Key.Type.ENTER -> onKeyEvent?.invoke(KeyEvent.Enter)
            Key.Type.SPACE -> onKeyEvent?.invoke(KeyEvent.Space)
            Key.Type.SHIFT -> {
                mode = when (mode) {
                    InputMode.NORMAL -> InputMode.SHIFTED
                    InputMode.SHIFTED -> InputMode.CAPS
                    InputMode.CAPS -> InputMode.NORMAL
                    else -> mode
                }
                invalidate()
            }

            Key.Type.NUMBER_TOGGLE -> {
                mode = if (mode == InputMode.NUMBERS) InputMode.NORMAL else InputMode.NUMBERS
                invalidate()
            }

            Key.Type.CHAR -> {
                val raw = when (mode) {
                    InputMode.NORMAL -> key.label
                    InputMode.SHIFTED -> key.shiftLabel
                    InputMode.CAPS -> key.label.uppercase()
                    InputMode.NUMBERS -> key.numLabel
                }
                raw.firstOrNull()?.let { onKeyEvent?.invoke(KeyEvent.Char(it)) }
                if (mode == InputMode.SHIFTED) {
                    mode = InputMode.NORMAL
                    invalidate()
                }
            }
        }
    }
}
