package com.shmulf.typer

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

object Ui {
    val BG = 0xFF070000.toInt()
    val PANEL = 0xF2120000.toInt()
    val FIELD = 0xFF1A0000.toInt()
    val RED = 0xFFD10000.toInt()
    val BRIGHT = 0xFFFF1F1F.toInt()
    val DARKRED = 0xFF6B0000.toInt()
    val DIM = 0xFFB07070.toInt()
    val TEXT = 0xFFF0E6E6.toInt()
    const val OWNER = "owner : ozuxe , tgk : nevqi"

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun box(c: Context, fill: Int, stroke: Int, radius: Int, strokeDp: Int = 2): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = dp(c, radius).toFloat()
        g.setStroke(dp(c, strokeDp), stroke)
        return g
    }

    fun title(c: Context, size: Float): TextView {
        val t = TextView(c)
        t.text = "SHMULF TYPE"
        t.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        t.setTextColor(BRIGHT)
        t.textSize = size
        t.letterSpacing = 0.12f
        t.setShadowLayer(18f, 0f, 0f, 0xFFFF0000.toInt())
        return t
    }

    fun label(c: Context, text: String): TextView {
        val t = TextView(c)
        t.text = text
        t.setTextColor(DIM)
        t.textSize = 12f
        return t
    }

    fun button(c: Context, text: String): Button {
        val b = Button(c)
        b.text = text
        b.setTextColor(0xFFFFFFFF.toInt())
        b.isAllCaps = false
        b.stateListAnimator = null
        b.background = btnBg(c, false)
        return b
    }

    fun btnBg(c: Context, dark: Boolean): GradientDrawable {
        val top = if (dark) 0xFF3A0000.toInt() else 0xFFE60000.toInt()
        val bot = if (dark) 0xFF150000.toInt() else 0xFF6B0000.toInt()
        val g = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bot))
        g.cornerRadius = dp(c, 12).toFloat()
        g.setStroke(dp(c, 2), BRIGHT)
        return g
    }

    fun edit(c: Context): EditText {
        val e = EditText(c)
        e.setTextColor(TEXT)
        e.setHintTextColor(DIM)
        e.background = box(c, FIELD, DARKRED, 10, 2)
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10))
        return e
    }

    fun footer(c: Context): TextView {
        val t = TextView(c)
        t.text = OWNER
        t.setTextColor(RED)
        t.textSize = 12f
        t.gravity = Gravity.CENTER
        t.letterSpacing = 0.06f
        return t
    }
}

/** Кровавая «капель» сверху. */
class DripView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val xs = floatArrayOf(0.03f, 0.10f, 0.18f, 0.27f, 0.36f, 0.45f, 0.55f, 0.64f, 0.73f, 0.82f, 0.90f, 0.97f)
    private val ls = floatArrayOf(0.55f, 0.95f, 0.40f, 0.75f, 0.50f, 1.00f, 0.35f, 0.80f, 0.55f, 0.90f, 0.45f, 0.70f)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        p.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(), 0xFFFF1A1A.toInt(), 0xFF5A0000.toInt(), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawRect(0f, 0f, w, h * 0.22f, p)
        for (i in xs.indices) {
            val x = w * xs[i]
            val len = h * ls[i] * 0.82f
            val r = h * (0.045f + 0.012f * (i % 3))
            c.drawRoundRect(RectF(x - r, 0f, x + r, len), r, r, p)
            c.drawCircle(x, len, r * 1.5f, p)
        }
    }
}
