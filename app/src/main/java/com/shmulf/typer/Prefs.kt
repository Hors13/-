package com.shmulf.typer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Tpl(var name: String, var text: String)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("shmulf", Context.MODE_PRIVATE)

    fun templates(c: Context): MutableList<Tpl> {
        val out = mutableListOf<Tpl>()
        val raw = sp(c).getString("tpls", null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(Tpl(o.getString("n"), o.getString("t")))
                }
            } catch (_: Exception) {
            }
        }
        if (out.isEmpty()) out.add(Tpl("Шаблон 1", "привет\nкак\nдела"))
        return out
    }

    fun saveTemplates(c: Context, list: List<Tpl>) {
        val arr = JSONArray()
        for (t in list) {
            val o = JSONObject()
            o.put("n", t.name)
            o.put("t", t.text)
            arr.put(o)
        }
        sp(c).edit().putString("tpls", arr.toString()).apply()
    }

    fun selected(c: Context): Int = sp(c).getInt("sel", 0)
    fun setSelected(c: Context, i: Int) = sp(c).edit().putInt("sel", i).apply()

    fun wpmLo(c: Context): Int = sp(c).getInt("lo", 60)
    fun wpmHi(c: Context): Int = sp(c).getInt("hi", 80)
    fun err(c: Context): Int = sp(c).getInt("err", 0)
    fun pauseSec(c: Context): Float = sp(c).getFloat("pause", 1f)
    fun loop(c: Context): Boolean = sp(c).getBoolean("loop", false)
    fun send(c: Context): Boolean = sp(c).getBoolean("send", true)

    fun setSpeed(c: Context, lo: Int, hi: Int) {
        sp(c).edit().putInt("lo", lo).putInt("hi", hi).apply()
    }

    fun saveSettings(c: Context, lo: Int, hi: Int, err: Int, pause: Float, loop: Boolean, send: Boolean) {
        sp(c).edit()
            .putInt("lo", lo).putInt("hi", hi).putInt("err", err)
            .putFloat("pause", pause).putBoolean("loop", loop).putBoolean("send", send)
            .apply()
    }
}
