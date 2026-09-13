package com.dualskin.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val engine = CalculatorEngine()
    private val history = mutableListOf<String>()

    private var shiftOn = false
    private var hypOn = false
    private var showingScientific = true
    private var memory: Double = 0.0

    private lateinit var display: EditText
    private lateinit var resultView: TextView
    private lateinit var statusView: TextView
    private lateinit var llScientific: LinearLayout
    private lateinit var llMinimal: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display)
        resultView = findViewById(R.id.resultView)
        statusView = findViewById(R.id.statusView)
        llScientific = findViewById(R.id.llScientific)
        llMinimal = findViewById(R.id.llMinimal)

        findViewById<Button>(R.id.btnToggleSkin).setOnClickListener { toggleSkin() }

        wireKeypad(llScientific)
        wireKeypad(llMinimal)

        updateStatus()
        showScientific(true)
    }

    private fun wireKeypad(root: ViewGroup) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is ViewGroup) {
                wireKeypad(child)
            } else if (child is Button) {
                val tag = child.tag as? String
                if (tag != null) {
                    child.setOnClickListener { onKey(tag) }
                }
            }
        }
    }

    private fun toggleSkin() {
        showScientific(!showingScientific)
    }

    private fun showScientific(sci: Boolean) {
        showingScientific = sci
        llScientific.visibility = if (sci) View.VISIBLE else View.GONE
        llMinimal.visibility = if (sci) View.GONE else View.VISIBLE
        if (sci) {
            display.setBackgroundColor(ContextCompat.getColor(this, R.color.sci_display_bg))
            display.setTextColor(Color.BLACK)
            statusView.visibility = View.VISIBLE
        } else {
            display.setBackgroundColor(ContextCompat.getColor(this, R.color.minimal_bg))
            display.setTextColor(ContextCompat.getColor(this, R.color.minimal_text))
            statusView.visibility = View.GONE
        }
    }

    private fun cursor(): Int = display.selectionStart.coerceIn(0, display.text.length)

    private fun insertAtCursor(token: String) {
        val p = cursor()
        display.text.insert(p, token)
        display.setSelection(p + token.length)
    }

    private fun onKey(tag: String) {
        when {
            tag.startsWith("INS:") -> insertAtCursor(tag.removePrefix("INS:"))
            tag.startsWith("FUN:") -> handleFunctionKey(tag.removePrefix("FUN:"))
            tag.startsWith("ACT:") -> handleAction(tag.removePrefix("ACT:"))
        }
    }

    private fun handleFunctionKey(name: String) {
        if (name == "log" && shiftOn) { insertAtCursor("10^("); clearShift(); return }
        if (name == "ln" && shiftOn) { insertAtCursor("exp("); clearShift(); return }

        val prefix = when (name) {
            "sin" -> when { shiftOn && hypOn -> "asinh("; hypOn -> "sinh("; shiftOn -> "asin("; else -> "sin(" }
            "cos" -> when { shiftOn && hypOn -> "acosh("; hypOn -> "cosh("; shiftOn -> "acos("; else -> "cos(" }
            "tan" -> when { shiftOn && hypOn -> "atanh("; hypOn -> "tanh("; shiftOn -> "atan("; else -> "tan(" }
            "sqrt" -> if (shiftOn) "cbrt(" else "sqrt("
            "logxy" -> "logxy("
            else -> "$name("
        }
        insertAtCursor(prefix)
        clearShift()
    }

    private fun clearShift() {
        if (shiftOn) { shiftOn = false; updateStatus() }
    }

    private fun handleAction(action: String) {
        when (action) {
            "AC" -> { display.setText(""); resultView.text = "" }
            "DEL" -> {
                val p = cursor()
                if (p > 0) display.text.delete(p - 1, p)
            }
            "LEFT" -> display.setSelection((cursor() - 1).coerceAtLeast(0))
            "RIGHT" -> display.setSelection((cursor() + 1).coerceAtMost(display.text.length))
            "UP", "DOWN" -> toast("مش متاح في النسخة دي")
            "SHIFT", "2ND" -> { shiftOn = !shiftOn; updateStatus() }
            "ALPHA" -> toast("وضع ALPHA (الحروف) مش مفعّل في النسخة دي")
            "HYP" -> { hypOn = !hypOn; updateStatus() }
            "MODE" -> {
                engine.angleMode = if (engine.angleMode == CalculatorEngine.AngleMode.DEG)
                    CalculatorEngine.AngleMode.RAD else CalculatorEngine.AngleMode.DEG
                updateStatus()
            }
            "ANS" -> insertAtCursor("Ans")
            "MPLUS" -> { memory += currentValueOrZero(); toast("الذاكرة الآن: ${formatNumber(memory)}") }
            "MMINUS" -> { memory -= currentValueOrZero(); toast("الذاكرة الآن: ${formatNumber(memory)}") }
            "MR" -> insertAtCursor(formatNumber(memory))
            "MSTO" -> { memory = currentValueOrZero(); toast("اتخزن في الذاكرة: ${formatNumber(memory)}") }
            "MC" -> { memory = 0.0; toast("اتمسحت الذاكرة") }
            "COPY" -> copyToClipboard()
            "PASTE" -> pasteFromClipboard()
            "HISTORY" -> showHistory()
            "HELP" -> showHelp()
            "EQUALS" -> doEquals()
            "STUB" -> toast("الوظيفة دي (حل معادلات / تفاضل وتكامل / مصفوفات / إحصاء...) مش متاحة في النسخة دي")
            "TOGGLESKIN" -> toggleSkin()
            else -> toast("غير مدعوم")
        }
    }

    private fun currentValueOrZero(): Double =
        try { engine.evaluate(display.text.toString()) } catch (e: Exception) { 0.0 }

    private fun copyToClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("calc", display.text.toString()))
        toast("اتنسخ")
    }

    private fun pasteFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val t = clip.getItemAt(0).coerceToText(this).toString()
            insertAtCursor(t)
        }
    }

    private fun doEquals() {
        val expr = display.text.toString()
        if (expr.isBlank()) return
        try {
            val value = engine.evaluate(expr)
            val formatted = formatNumber(value)
            resultView.text = "= $formatted"
            history.add(0, "$expr = $formatted")
            if (history.size > 50) history.removeAt(history.size - 1)
            display.setText(formatted)
            display.setSelection(display.text.length)
        } catch (e: Exception) {
            resultView.text = "خطأ: ${e.message ?: "تعبير غير صالح"}"
        }
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN()) return "غير معرّف"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        return if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            var s = String.format("%.10f", value)
            s = s.trimEnd('0').trimEnd('.')
            s
        }
    }

    private fun updateStatus() {
        val angle = if (engine.angleMode == CalculatorEngine.AngleMode.DEG) "DEG" else "RAD"
        val shift = if (shiftOn) " • SHIFT" else ""
        val hyp = if (hypOn) " • HYP" else ""
        statusView.text = "$angle$shift$hyp"
    }

    private fun showHistory() {
        if (history.isEmpty()) { toast("مفيش عمليات محفوظة لسه"); return }
        AlertDialog.Builder(this)
            .setTitle("السجل")
            .setItems(history.toTypedArray()) { _, which ->
                val exprPart = history[which].substringBefore(" = ")
                display.setText(exprPart)
                display.setSelection(display.text.length)
            }
            .setPositiveButton("قفل", null)
            .show()
    }

    private fun showHelp() {
        val msg = "الدوال المتاحة فعليًا:\n" +
            "+ - × ÷ ^ ( ) mod ! %\n" +
            "sin cos tan (و SHIFT بيدّي الدالة العكسية)\n" +
            "hyp بيحوّلهم لـ sinh/cosh/tanh\n" +
            "log (أساس 10) ، ln (أساس e)\n" +
            "SHIFT+log = 10^x ، SHIFT+ln = eˣ\n" +
            "sqrt ، SHIFT+sqrt = الجذر التكعيبي\n" +
            "x² ، x⁻¹ ، xʸ ، Logₓy(x,y)\n" +
            "nCr(n,r) ، nPr(n,r) ، gcd(a,b) ، lcm(a,b)\n" +
            "Ans ، M+ ، M− ، RCL ، STO ، MC\n" +
            "Copy / Paste / History\n\n" +
            "مش متاح في النسخة دي (الزرار هيوريك تنبيه):\n" +
            "SOLVE، التفاضل والتكامل (d/dx, ∫dx)، المصفوفات،\n" +
            "المتجهات، الأعداد المركبة، الإحصاء والتوزيعات،\n" +
            "S⇌D، ENG، °′″."
        AlertDialog.Builder(this)
            .setTitle("مساعدة")
            .setMessage(msg)
            .setPositiveButton("تمام", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
