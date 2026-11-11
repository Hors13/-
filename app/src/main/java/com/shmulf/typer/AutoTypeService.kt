package com.shmulf.typer

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.random.Random

class AutoTypeService : AccessibilityService() {

    companion object {
        var instance: AutoTypeService? = null
    }

    private lateinit var wm: WindowManager
    private lateinit var lp: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())

    private var rootV: FrameLayout? = null
    private var fullV: View? = null
    private var miniV: LinearLayout? = null
    private var miniL: TextView? = null
    private var statusTv: TextView? = null
    private var tplTv: TextView? = null
    private var startBtn: Button? = null
    private var loTv: TextView? = null
    private var hiTv: TextView? = null

    // состояние печати
    private var running = false
    private var msgs: List<Msg> = emptyList()
    private var mi = 0
    private var curMsg: Msg? = null
    private var target = ""
    private var pos = 0
    private var wpm = 60
    private var sent = 0

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        rootV?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        rootV = null
        instance = null
        super.onDestroy()
    }

    // ================= Плавающая панель =================

    fun showPanel() {
        if (rootV != null) return
        val c = this
        fun dp(v: Int) = Ui.dp(c, v)

        val full = LinearLayout(c)
        full.orientation = LinearLayout.VERTICAL
        full.background = Ui.box(c, Ui.PANEL, Ui.RED, 16, 2)
        full.minimumWidth = dp(270)
        full.setPadding(dp(12), dp(6), dp(12), dp(8))

        val drip = DripView(c)
        val dripLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18))
        dripLp.leftMargin = dp(8)
        dripLp.rightMargin = dp(8)
        full.addView(drip, dripLp)

        val header = LinearLayout(c)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val title = Ui.title(c, 17f)
        val minBtn = TextView(c)
        minBtn.text = "—"
        minBtn.setTextColor(Ui.BRIGHT)
        minBtn.textSize = 24f
        minBtn.setPadding(dp(14), 0, dp(4), 0)
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(minBtn)
        full.addView(header)

        val st = TextView(c)
        st.text = "Готов"
        st.setTextColor(Ui.DIM)
        st.textSize = 12f
        full.addView(st, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(2) })

        // выбор шаблона ◀ имя ▶
        val tplRow = LinearLayout(c)
        tplRow.orientation = LinearLayout.HORIZONTAL
        tplRow.gravity = Gravity.CENTER_VERTICAL
        val prev = TextView(c)
        prev.text = "◀"
        prev.setTextColor(Ui.BRIGHT)
        prev.textSize = 18f
        prev.setPadding(dp(8), dp(6), dp(12), dp(6))
        val tplName = TextView(c)
        tplName.setTextColor(Ui.TEXT)
        tplName.textSize = 14f
        tplName.gravity = Gravity.CENTER
        tplName.maxLines = 1
        val next = TextView(c)
        next.text = "▶"
        next.setTextColor(Ui.BRIGHT)
        next.textSize = 18f
        next.setPadding(dp(12), dp(6), dp(8), dp(6))
        tplRow.addView(prev)
        tplRow.addView(tplName, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        tplRow.addView(next)
        full.addView(tplRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(4) })

        // скорость прямо в окне: от [−] 60 [+]  до [−] 80 [+]
        val spLabel = Ui.label(c, "СКОРОСТЬ, ВПМ")
        spLabel.gravity = Gravity.CENTER
        full.addView(spLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(6) })

        fun stepBtn(txt: String, isHi: Boolean, d: Int): TextView {
            val t = TextView(c)
            t.text = txt
            t.setTextColor(Ui.BRIGHT)
            t.textSize = 22f
            t.gravity = Gravity.CENTER
            t.background = Ui.box(c, Ui.FIELD, Ui.DARKRED, 8, 1)
            t.setOnClickListener { stepSpeed(isHi, d) }
            return t
        }
        fun valTv(): TextView {
            val t = TextView(c)
            t.setTextColor(Ui.TEXT)
            t.textSize = 16f
            t.gravity = Gravity.CENTER
            t.minWidth = dp(40)
            return t
        }
        fun small(txt: String): TextView {
            val t = Ui.label(c, txt)
            t.setPadding(dp(6), 0, dp(4), 0)
            return t
        }
        val lo = valTv()
        val hi = valTv()
        val speedRow = LinearLayout(c)
        speedRow.orientation = LinearLayout.HORIZONTAL
        speedRow.gravity = Gravity.CENTER
        speedRow.addView(small("от"))
        speedRow.addView(stepBtn("−", false, -5), LinearLayout.LayoutParams(dp(34), dp(34)))
        speedRow.addView(lo)
        speedRow.addView(stepBtn("+", false, 5), LinearLayout.LayoutParams(dp(34), dp(34)))
        speedRow.addView(small("  до"))
        speedRow.addView(stepBtn("−", true, -5), LinearLayout.LayoutParams(dp(34), dp(34)))
        speedRow.addView(hi)
        speedRow.addView(stepBtn("+", true, 5), LinearLayout.LayoutParams(dp(34), dp(34)))
        full.addView(speedRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(4) })
        loTv = lo
        hiTv = hi

        val btn = Ui.button(c, "▶  СТАРТ")
        btn.setOnClickListener { toggle() }
        full.addView(btn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).also { it.topMargin = dp(6) })

        val foot = Ui.footer(c)
        foot.textSize = 10f
        full.addView(foot, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(8) })

        // свёрнутая пилюля: [▶/■ | ↗]
        val mini = LinearLayout(c)
        mini.orientation = LinearLayout.HORIZONTAL
        mini.gravity = Gravity.CENTER
        mini.visibility = View.GONE
        val mL = TextView(c)
        mL.text = "▶"
        mL.setTextColor(Ui.BRIGHT)
        mL.textSize = 20f
        mL.gravity = Gravity.CENTER
        mL.setPadding(dp(16), dp(8), dp(16), dp(8))
        val mR = TextView(c)
        mR.text = "↗"
        mR.setTextColor(Ui.TEXT)
        mR.textSize = 20f
        mR.gravity = Gravity.CENTER
        mR.setPadding(dp(16), dp(8), dp(16), dp(8))
        mini.addView(mL, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        mini.addView(mR, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        mini.minimumWidth = dp(104)

        val root = FrameLayout(c)
        root.addView(full)
        root.addView(mini)

        lp = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // чтобы поле ввода не теряло фокус
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = dp(16)
        lp.y = dp(180)

        minBtn.setOnClickListener {
            full.visibility = View.GONE
            mini.visibility = View.VISIBLE
        }
        prev.setOnClickListener { switchTemplate(-1) }
        next.setOnClickListener { switchTemplate(1) }
        attachDrag(header) { _, _ -> }
        attachDrag(mini) { x, w ->
            if (x < w / 2f) {
                toggle()
            } else {
                mini.visibility = View.GONE
                full.visibility = View.VISIBLE
            }
        }

        rootV = root
        fullV = full
        miniV = mini
        miniL = mL
        statusTv = st
        tplTv = tplName
        startBtn = btn
        wm.addView(root, lp)
        refreshTpl()
        refreshSpeed()
        refreshUi()
    }

    private fun attachDrag(v: View, onTap: (Float, Int) -> Unit) {
        var sx = 0f
        var sy = 0f
        var ox = 0
        var oy = 0
        var moved = false
        v.setOnTouchListener { view, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - sx).toInt()
                    val dy = (e.rawY - sy).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    lp.x = ox + dx
                    lp.y = oy + dy
                    rootV?.let { wm.updateViewLayout(it, lp) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap(e.x, view.width)
                    true
                }
                else -> false
            }
        }
    }

    private fun refreshUi() {
        val btn = startBtn
        if (btn != null) {
            btn.text = if (running) "■  СТОП" else "▶  СТАРТ"
            btn.background = Ui.btnBg(this, running)
        }
        miniL?.text = if (running) "■" else "▶"
        miniV?.background = Ui.box(this, if (running) Ui.DARKRED else Ui.PANEL, Ui.RED, 24, 2)
    }

    private fun refreshTpl() {
        val l = Prefs.templates(this)
        val i = Prefs.selected(this).coerceIn(0, l.size - 1)
        tplTv?.text = l[i].name
    }

    private fun refreshSpeed() {
        loTv?.text = Prefs.wpmLo(this).toString()
        hiTv?.text = Prefs.wpmHi(this).toString()
    }

    /** Шаг ±5 впм; "от" не может быть больше "до". Работает и во время печати. */
    private fun stepSpeed(isHi: Boolean, d: Int) {
        var lo = Prefs.wpmLo(this)
        var hi = Prefs.wpmHi(this)
        if (isHi) hi = (hi + d).coerceIn(5, 400) else lo = (lo + d).coerceIn(5, 400)
        if (lo > hi) { if (isHi) lo = hi else hi = lo }
        Prefs.setSpeed(this, lo, hi)
        refreshSpeed()
    }

    /** Диапазон скорости: из шаблона (*60-80впм*), иначе — с панели в реальном времени. */
    private fun rangeFor(m: Msg): IntRange =
        if (m.ownSpeed) m.lo..m.hi else Prefs.wpmLo(this)..Prefs.wpmHi(this)

    private fun rollWpm(m: Msg): Int {
        val r = rangeFor(m)
        return Random.nextInt(r.first, r.last + 1)
    }

    private fun switchTemplate(d: Int) {
        if (running) { status("Сначала остановись"); return }
        val l = Prefs.templates(this)
        val i = (Prefs.selected(this).coerceIn(0, l.size - 1) + d + l.size) % l.size
        Prefs.setSelected(this, i)
        refreshTpl()
    }

    private fun status(s: String) {
        statusTv?.text = s
    }

    // ================= Печать =================

    private val stepper = Runnable { step() }

    private fun toggle() {
        if (running) stop("Остановлено") else start()
    }

    private fun start() {
        val l = Prefs.templates(this)
        val t = l[Prefs.selected(this).coerceIn(0, l.size - 1)]
        msgs = Parser.parse(t.text, Prefs.wpmLo(this), Prefs.wpmHi(this), Prefs.err(this))
        if (msgs.isEmpty()) {
            status("Шаблон пуст")
            return
        }
        running = true
        mi = 0
        curMsg = null
        sent = 0
        refreshUi()
        handler.removeCallbacksAndMessages(null)
        handler.post(stepper)
    }

    private fun stop(text: String) {
        running = false
        handler.removeCallbacksAndMessages(null)
        refreshUi()
        status(text)
    }

    private fun step() {
        if (!running) return
        val cm = curMsg
        if (cm == null) {
            beginMessage()
            return
        }
        val field = findField()
        if (field == null) {
            status("Тапни в поле ввода")
            handler.postDelayed(stepper, 600)
            return
        }
        val next = pos + 1
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, target.substring(0, next))
        if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            status("Поле не принимает текст")
            handler.postDelayed(stepper, 800)
            return
        }
        pos = next
        if (pos >= target.length) {
            handler.postDelayed({ finishMessage() }, Random.nextLong(180, 320))
            return
        }
        val ch = target[pos - 1]
        if (ch == ' ') wpm = rollWpm(cm)
        handler.postDelayed(stepper, charDelay(ch))
    }

    private fun beginMessage() {
        if (mi >= msgs.size) {
            if (Prefs.loop(this)) mi = 0 else { stop("Готово: отправлено $sent"); return }
        }
        val m = msgs[mi]
        curMsg = m
        target = Typo.mutate(m.text, m.err)
        pos = 0
        wpm = rollWpm(m)
        status("Сообщение ${mi + 1}/${msgs.size} · $wpm впм · ошибки ${m.err}%")
        handler.postDelayed(stepper, Random.nextLong(350, 800))
    }

    private fun finishMessage() {
        if (!running) return
        var ok = true
        if (Prefs.send(this)) ok = pressSend()
        if (ok) sent++
        status(if (ok) "Отправлено: $sent" else "Не нашёл «Отправить»")
        curMsg = null
        mi++
        val p = (Prefs.pauseSec(this) * 1000 * Random.nextDouble(0.7, 1.3)).toLong().coerceAtLeast(200L)
        handler.postDelayed(stepper, p)
    }

    /** 60000 / (впм * 5) мс на символ, плюс «человеческий» разброс. */
    private fun charDelay(ch: Char): Long {
        var d = 60000.0 / (wpm * 5.0) * Random.nextDouble(0.6, 1.4)
        if (ch == ' ') d *= 1.2
        if (ch == ',' || ch == '.' || ch == '!' || ch == '?') d *= 1.8
        if (Random.nextInt(100) < 3) d *= 3.0
        return d.toLong().coerceAtLeast(20L)
    }

    // ================= Работа с чужим окном =================

    private fun findField(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (f != null && f.isEditable) return f
        return findFocusedEditable(root)
    }

    private fun findFocusedEditable(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (n.isEditable && n.isFocused) return n
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            findFocusedEditable(c)?.let { return it }
        }
        return null
    }

    private fun pressSend(): Boolean {
        val root = rootInActiveWindow ?: return false
        val btn = findSendButton(root)
        if (btn != null) {
            var n: AccessibilityNodeInfo? = btn
            while (n != null && !n.isClickable) n = n.parent
            if (n != null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        // запасной вариант (браузеры и т.п.): Enter на клавиатуре, Android 11+
        if (Build.VERSION.SDK_INT >= 30) {
            val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            return field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
        }
        return false
    }

    private fun findSendButton(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (!n.isEditable) {
            val label = (n.contentDescription ?: n.text ?: "").toString().lowercase()
            val id = n.viewIdResourceName?.lowercase() ?: ""
            if (label.startsWith("send") || label.startsWith("отправ") ||
                id.endsWith("/send") || id.contains("send_button") || id.contains("send_btn")
            ) return n
        }
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            findSendButton(c)?.let { return it }
        }
        return null
    }
}
