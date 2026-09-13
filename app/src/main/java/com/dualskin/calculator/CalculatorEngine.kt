package com.dualskin.calculator

import kotlin.math.PI
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * Small hand-written recursive-descent expression evaluator.
 *
 * Supported: + - * / ^ ( ) , mod, postfix ! and %, unary +/-,
 * functions sin cos tan asin acos atan sinh cosh tanh asinh acosh atanh
 * cot acot log ln sqrt cbrt abs ceil floor exp gcd lcm nCr nPr logxy,
 * constants pi / e / Ans.
 *
 * This is NOT a full CAS: there is no support for solving equations,
 * derivatives/integrals, matrices, vectors, complex numbers or
 * statistical distributions. Buttons for those show a "not available"
 * message in the UI instead of pretending to compute something.
 */
class CalculatorEngine {

    enum class AngleMode { DEG, RAD }

    var angleMode: AngleMode = AngleMode.DEG
    var lastAnswer: Double = 0.0

    private var text: String = ""
    private var pos: Int = 0

    fun evaluate(expression: String): Double {
        text = expression.replace(" ", "")
        pos = 0
        if (text.isBlank()) return 0.0
        val result = parseExpression()
        if (pos != text.length) {
            throw IllegalArgumentException("رمز غير متوقع عند الموضع $pos")
        }
        lastAnswer = result
        return result
    }

    private fun peek(): Char? = text.getOrNull(pos)

    private fun consume(expected: Char) {
        if (peek() != expected) throw IllegalArgumentException("متوقع '$expected' عند $pos")
        pos++
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            when (peek()) {
                '+' -> { pos++; value += parseTerm() }
                '-' -> { pos++; value -= parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseUnary()
        while (true) {
            if (matchWord("mod")) {
                value %= parseUnary()
                continue
            }
            when (peek()) {
                '*' -> { pos++; value *= parseUnary() }
                '/' -> {
                    pos++
                    val d = parseUnary()
                    if (d == 0.0) throw ArithmeticException("القسمة على صفر")
                    value /= d
                }
                else -> return value
            }
        }
    }

    private fun parseUnary(): Double {
        return when (peek()) {
            '-' -> { pos++; -parseUnary() }
            '+' -> { pos++; parseUnary() }
            else -> parsePower()
        }
    }

    private fun parsePower(): Double {
        val base = parsePostfix()
        if (peek() == '^') {
            pos++
            val exponent = parseUnary()
            return base.pow(exponent)
        }
        return base
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()
        while (true) {
            when (peek()) {
                '!' -> { pos++; value = factorial(value) }
                '%' -> { pos++; value /= 100.0 }
                else -> return value
            }
        }
    }

    private fun parsePrimary(): Double {
        val c = peek() ?: throw IllegalArgumentException("نهاية غير متوقعة للتعبير")

        if (c == '(') {
            pos++
            val v = parseExpression()
            consume(')')
            return v
        }

        if (c.isDigit() || c == '.') {
            return parseNumber()
        }

        if (c.isLetter()) {
            val name = parseIdentifier()
            return when (name.lowercase()) {
                "pi" -> PI
                "e" -> E
                "ans" -> lastAnswer
                else -> {
                    consume('(')
                    val args = mutableListOf<Double>()
                    args.add(parseExpression())
                    while (peek() == ',') { pos++; args.add(parseExpression()) }
                    consume(')')
                    callFunction(name.lowercase(), args)
                }
            }
        }

        throw IllegalArgumentException("رمز غير متوقع '$c' عند $pos")
    }

    private fun parseNumber(): Double {
        val start = pos
        while (peek()?.isDigit() == true) pos++
        if (peek() == '.') {
            pos++
            while (peek()?.isDigit() == true) pos++
        }
        if (peek() == 'e' || peek() == 'E') {
            val save = pos
            pos++
            if (peek() == '+' || peek() == '-') pos++
            if (peek()?.isDigit() == true) {
                while (peek()?.isDigit() == true) pos++
            } else {
                pos = save
            }
        }
        return text.substring(start, pos).toDouble()
    }

    private fun parseIdentifier(): String {
        val start = pos
        while (peek()?.isLetter() == true) pos++
        return text.substring(start, pos)
    }

    private fun matchWord(word: String): Boolean {
        if (text.regionMatches(pos, word, 0, word.length, ignoreCase = true)) {
            val after = text.getOrNull(pos + word.length)
            if (after == null || !after.isLetter()) {
                pos += word.length
                return true
            }
        }
        return false
    }

    private fun toRadians(x: Double) = if (angleMode == AngleMode.DEG) Math.toRadians(x) else x
    private fun fromRadians(x: Double) = if (angleMode == AngleMode.DEG) Math.toDegrees(x) else x

    private fun factorial(x: Double): Double {
        if (x < 0 || x != floor(x)) throw ArithmeticException("! يحتاج عدد صحيح موجب")
        var result = 1.0
        var i = 2
        val n = x.toInt()
        while (i <= n) { result *= i; i++ }
        return result
    }

    private fun gcdLong(a: Long, b: Long): Long {
        var x = abs(a); var y = abs(b)
        while (y != 0L) { val t = y; y = x % y; x = t }
        return x
    }

    private fun callFunction(name: String, args: List<Double>): Double {
        fun one() = args[0]
        return when (name) {
            "sin" -> sin(toRadians(one()))
            "cos" -> cos(toRadians(one()))
            "tan" -> tan(toRadians(one()))
            "asin" -> fromRadians(asin(one()))
            "acos" -> fromRadians(acos(one()))
            "atan" -> fromRadians(atan(one()))
            "sinh" -> sinh(one())
            "cosh" -> cosh(one())
            "tanh" -> tanh(one())
            "asinh" -> asinh(one())
            "acosh" -> acosh(one())
            "atanh" -> atanh(one())
            "cot" -> 1.0 / tan(toRadians(one()))
            "acot" -> fromRadians(atan(1.0 / one()))
            "log" -> log10(one())
            "ln" -> ln(one())
            "sqrt" -> sqrt(one())
            "cbrt" -> cbrt(one())
            "abs" -> abs(one())
            "ceil" -> ceil(one())
            "floor" -> floor(one())
            "exp" -> exp(one())
            "logxy" -> ln(args[1]) / ln(args[0])
            "gcd" -> gcdLong(args[0].toLong(), args[1].toLong()).toDouble()
            "lcm" -> {
                val a = args[0].toLong(); val b = args[1].toLong()
                if (a == 0L || b == 0L) 0.0 else abs(a * b) / gcdLong(a, b).toDouble()
            }
            "ncr" -> {
                val n = args[0]; val r = args[1]
                factorial(n) / (factorial(r) * factorial(n - r))
            }
            "npr" -> {
                val n = args[0]; val r = args[1]
                factorial(n) / factorial(n - r)
            }
            else -> throw IllegalArgumentException("الدالة '$name' غير مدعومة")
        }
    }
}
