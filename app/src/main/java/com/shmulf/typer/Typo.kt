package com.shmulf.typer

import kotlin.random.Random

data class Msg(val text: String, val lo: Int, val hi: Int, val err: Int, val ownSpeed: Boolean)

/** Разбор шаблона: каждая строка = сообщение; *60-80впм* и *50%* — настройки на лету. */
object Parser {
    private val tag = Regex("\\*([^*\\n]{1,40})\\*")
    private val nums = Regex("\\d+")

    fun parse(src: String, defLo: Int, defHi: Int, defErr: Int): List<Msg> {
        var lo = defLo
        var hi = defHi
        var err = defErr
        var own = false
        val out = mutableListOf<Msg>()
        for (line in src.split("\n")) {
            val clean = tag.replace(line) { m ->
                val inner = m.groupValues[1].lowercase()
                if (inner.contains("впм") || inner.contains("wpm")) {
                    val n = nums.findAll(inner).map { it.value.toInt() }.toList()
                    if (n.isNotEmpty()) {
                        lo = n[0]
                        hi = if (n.size > 1) n[1] else n[0]
                        if (hi < lo) { val t = lo; lo = hi; hi = t }
                        lo = lo.coerceIn(5, 400)
                        hi = hi.coerceIn(5, 400)
                        own = true
                    }
                    ""
                } else if (inner.contains("%")) {
                    val n = nums.find(inner)?.value?.toIntOrNull()
                    if (n != null) err = n.coerceIn(0, 100)
                    ""
                } else {
                    m.value
                }
            }.trim()
            if (clean.isNotEmpty()) out.add(Msg(clean, lo, hi, err, own))
        }
        return out
    }
}

/** Опечатки «промахнулся мимо клавиши»: соседняя клавиша, перестановка, двойная буква. */
object Typo {
    private val rows = listOf(
        "йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю",
        "qwertyuiop", "asdfghjkl", "zxcvbnm"
    )
    private val nb = HashMap<Char, List<Char>>()

    init {
        for (base in listOf(0, 3)) {
            for (r in 0..2) {
                val row = rows[base + r]
                for (i in row.indices) {
                    val list = mutableListOf<Char>()
                    if (i > 0) list.add(row[i - 1])
                    if (i < row.length - 1) list.add(row[i + 1])
                    if (r > 0) {
                        val up = rows[base + r - 1]
                        if (i < up.length) list.add(up[i])
                        if (i + 1 < up.length) list.add(up[i + 1])
                    }
                    if (r < 2) {
                        val dn = rows[base + r + 1]
                        if (i - 1 >= 0 && i - 1 < dn.length) list.add(dn[i - 1])
                        if (i < dn.length) list.add(dn[i])
                    }
                    nb[row[i]] = list
                }
            }
        }
    }

    /** errPct — шанс (в %), что в слове будет одна ошибка. */
    fun mutate(text: String, errPct: Int): String {
        if (errPct <= 0) return text
        val sb = StringBuilder()
        val cur = StringBuilder()
        fun flush() {
            if (cur.isNotEmpty()) {
                val w = cur.toString()
                sb.append(if (Random.nextInt(100) < errPct) mutateWord(w) else w)
                cur.setLength(0)
            }
        }
        for (c in text) {
            if (c.isLetter()) cur.append(c) else { flush(); sb.append(c) }
        }
        flush()
        return sb.toString()
    }

    private fun mutateWord(w: String): String {
        val idx = w.indices.filter { nb.containsKey(w[it].lowercaseChar()) }
        if (idx.size < 2) return w
        val pos = idx.random()
        val ch = w[pos]
        val roll = Random.nextInt(100)
        if (roll in 75..89 && pos < w.length - 1 && w[pos + 1] != ch) {
            return w.substring(0, pos) + w[pos + 1] + ch + w.substring(pos + 2)
        }
        if (roll >= 90) {
            return w.substring(0, pos + 1) + ch + w.substring(pos + 1)
        }
        val n = nb[ch.lowercaseChar()]!!.random()
        val rep = if (ch.isUpperCase()) n.uppercaseChar() else n
        return w.substring(0, pos) + rep + w.substring(pos + 1)
    }
}
