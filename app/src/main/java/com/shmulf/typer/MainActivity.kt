package com.shmulf.typer

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var tpls: MutableList<Tpl>
    private var cur = 0
    private lateinit var nameEt: EditText
    private lateinit var textEt: EditText
    private lateinit var spinner: Spinner
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var loEt: EditText
    private lateinit var hiEt: EditText
    private lateinit var errBar: SeekBar
    private lateinit var errTv: TextView
    private lateinit var pauseEt: EditText
    private lateinit var loopCb: CheckBox
    private lateinit var sendCb: CheckBox

    private fun lp(top: Int = 10) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).also { it.topMargin = Ui.dp(this, top) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG

        tpls = Prefs.templates(this)
        cur = Prefs.selected(this).coerceIn(0, tpls.size - 1)

        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        val pad = Ui.dp(this, 18)
        body.setPadding(pad, 0, pad, pad)

        val t = Ui.title(this, 36f)
        t.gravity = Gravity.CENTER
        body.addView(t, lp(2))
        val sub = Ui.label(this, "автонабор · человеческая печать")
        sub.gravity = Gravity.CENTER
        body.addView(sub, lp(2))

        // --- шаблоны ---
        body.addView(Ui.label(this, "ШАБЛОН"), lp(18))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        spinner = Spinner(this)
        spinner.background = Ui.box(this, Ui.FIELD, Ui.DARKRED, 10, 2)
        spinner.setPopupBackgroundDrawable(ColorDrawable(Ui.FIELD))
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tpls.map { it.name }.toMutableList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(cur)
        row.addView(spinner, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val newBtn = Ui.button(this, "＋")
        val delBtn = Ui.button(this, "✕")
        row.addView(newBtn, LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)).also { it.leftMargin = Ui.dp(this, 8) })
        row.addView(delBtn, LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)).also { it.leftMargin = Ui.dp(this, 8) })
        body.addView(row, lp(6))

        nameEt = Ui.edit(this)
        nameEt.hint = "Название шаблона"
        nameEt.setSingleLine(true)
        body.addView(nameEt, lp(8))

        textEt = Ui.edit(this)
        textEt.hint = "привет\nкак\nдела"
        textEt.minLines = 7
        textEt.gravity = Gravity.TOP
        textEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        body.addView(textEt, lp(8))

        body.addView(
            Ui.label(
                this,
                "Каждая строка — отдельное сообщение.\n" +
                    "Скорость обычно меняется на плавающей панели. Если нужно для части текста — пиши в шаблоне " +
                    "*60-80впм* (перебивает панель для строк ниже), *50%* — шанс ошибки в слове. " +
                    "Такие строки не отправляются."
            ), lp(6)
        )

        // --- настройки ---
        body.addView(Ui.label(this, "СКОРОСТЬ, ВПМ (от — до; её же меняет панель)"), lp(18))
        val speedRow = LinearLayout(this)
        speedRow.orientation = LinearLayout.HORIZONTAL
        speedRow.gravity = Gravity.CENTER_VERTICAL
        loEt = Ui.edit(this)
        hiEt = Ui.edit(this)
        loEt.inputType = InputType.TYPE_CLASS_NUMBER
        hiEt.inputType = InputType.TYPE_CLASS_NUMBER
        loEt.setText(Prefs.wpmLo(this).toString())
        hiEt.setText(Prefs.wpmHi(this).toString())
        val dash = TextView(this)
        dash.text = "  —  "
        dash.setTextColor(Ui.BRIGHT)
        speedRow.addView(loEt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        speedRow.addView(dash)
        speedRow.addView(hiEt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(speedRow, lp(6))

        errTv = TextView(this)
        errTv.setTextColor(Ui.DIM)
        errTv.textSize = 12f
        body.addView(errTv, lp(16))
        errBar = SeekBar(this)
        errBar.max = 100
        errBar.progress = Prefs.err(this)
        errBar.progressTintList = ColorStateList.valueOf(Ui.BRIGHT)
        errBar.thumbTintList = ColorStateList.valueOf(Ui.BRIGHT)
        errBar.progressBackgroundTintList = ColorStateList.valueOf(Ui.DARKRED)
        errTv.text = "ОШИБКИ ПО УМОЛЧАНИЮ: ${errBar.progress}% слов"
        errBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                errTv.text = "ОШИБКИ ПО УМОЛЧАНИЮ: $p% слов"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        body.addView(errBar, lp(4))

        body.addView(Ui.label(this, "ПАУЗА МЕЖДУ СООБЩЕНИЯМИ, СЕК"), lp(14))
        pauseEt = Ui.edit(this)
        pauseEt.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        pauseEt.setText(Prefs.pauseSec(this).toString())
        body.addView(pauseEt, lp(6))

        loopCb = CheckBox(this)
        loopCb.text = "Повторять шаблон по кругу"
        loopCb.setTextColor(Ui.TEXT)
        loopCb.buttonTintList = ColorStateList.valueOf(Ui.BRIGHT)
        loopCb.isChecked = Prefs.loop(this)
        body.addView(loopCb, lp(10))

        sendCb = CheckBox(this)
        sendCb.text = "Отправлять каждую строку (кнопка «Отправить» / Enter)"
        sendCb.setTextColor(Ui.TEXT)
        sendCb.buttonTintList = ColorStateList.valueOf(Ui.BRIGHT)
        sendCb.isChecked = Prefs.send(this)
        body.addView(sendCb, lp(2))

        // --- кнопки ---
        val saveBtn = Ui.button(this, "💾  СОХРАНИТЬ")
        val svcBtn = Ui.button(this, "☠  1. ВКЛЮЧИТЬ СЛУЖБУ")
        val panelBtn = Ui.button(this, "🩸  2. ПОКАЗАТЬ ПАНЕЛЬ")
        body.addView(saveBtn, lp(20))
        body.addView(svcBtn, lp(10))
        body.addView(panelBtn, lp(10))

        body.addView(
            Ui.label(
                this,
                "Открой чат или сайт, тапни в строку ввода (чтобы вылезла клавиатура) и жми ▶ СТАРТ на панели. " +
                    "«—» сворачивает панель в пилюлю: левая половина — старт/стоп, правая (↗) — развернуть."
            ), lp(14)
        )

        // --- события ---
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (position != cur) {
                    commit()
                    cur = position
                    load()
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        newBtn.setOnClickListener {
            commit()
            tpls.add(Tpl("Шаблон ${tpls.size + 1}", ""))
            cur = tpls.size - 1
            load()
            refreshSpinner()
        }
        delBtn.setOnClickListener {
            if (tpls.size > 1) {
                tpls.removeAt(cur)
                cur = cur.coerceAtMost(tpls.size - 1)
            } else {
                tpls[0] = Tpl("Шаблон 1", "")
            }
            load()
            refreshSpinner()
        }
        saveBtn.setOnClickListener {
            saveAll()
            refreshSpinner()
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
        }
        svcBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        panelBtn.setOnClickListener {
            saveAll()
            val s = AutoTypeService.instance
            if (s == null) {
                Toast.makeText(this, "Сначала включи службу ShmulfTyper в настройках", Toast.LENGTH_LONG).show()
            } else {
                s.showPanel()
                Toast.makeText(this, "Панель на экране. Открой мессенджер.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- сборка экрана ---
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.addView(DripView(this), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 60)))
        content.addView(body)
        val scroll = ScrollView(this)
        scroll.addView(content)

        val outer = LinearLayout(this)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Ui.BG)
        outer.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val foot = Ui.footer(this)
        foot.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 10))
        outer.addView(foot, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(outer)

        load()
    }

    private fun commit() {
        val n = nameEt.text.toString().ifBlank { "Шаблон ${cur + 1}" }
        tpls[cur] = Tpl(n, textEt.text.toString())
    }

    private fun load() {
        nameEt.setText(tpls[cur].name)
        textEt.setText(tpls[cur].text)
    }

    private fun refreshSpinner() {
        adapter.clear()
        adapter.addAll(tpls.map { it.name })
        adapter.notifyDataSetChanged()
        spinner.setSelection(cur)
    }

    private fun saveAll() {
        commit()
        Prefs.saveTemplates(this, tpls)
        Prefs.setSelected(this, cur)
        val lo = loEt.text.toString().toIntOrNull() ?: 60
        val hi = hiEt.text.toString().toIntOrNull() ?: 80
        val pause = pauseEt.text.toString().replace(',', '.').toFloatOrNull() ?: 1f
        Prefs.saveSettings(
            this,
            minOf(lo, hi).coerceIn(5, 400), maxOf(lo, hi).coerceIn(5, 400),
            errBar.progress, pause.coerceIn(0.2f, 60f),
            loopCb.isChecked, sendCb.isChecked
        )
    }

    override fun onResume() {
        super.onResume()
        // скорость могли поменять на плавающей панели
        loEt.setText(Prefs.wpmLo(this).toString())
        hiEt.setText(Prefs.wpmHi(this).toString())
    }

    override fun onPause() {
        saveAll()
        super.onPause()
    }
}
