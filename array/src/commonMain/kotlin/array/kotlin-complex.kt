package array.complex

import kotlin.math.*

data class Complex(val real: Double, val imaginary: Double) {

    constructor(value: Double) : this(value, 0.0)

    fun reciprocal(): Complex {
        val scale = (real * real) + (imaginary * imaginary)
        return Complex(real / scale, -imaginary / scale)
    }

    fun abs(): Double = hypot(real, imaginary)

    operator fun unaryMinus(): Complex = Complex(-real, -imaginary)
    operator fun plus(other: Double): Complex = Complex(real + other, imaginary)
    operator fun minus(other: Double): Complex = Complex(real - other, imaginary)
    operator fun times(other: Double): Complex = Complex(real * other, imaginary * other)
    operator fun div(other: Double): Complex = Complex(real / other, imaginary / other)

    operator fun plus(other: Complex): Complex =
        Complex(real + other.real, imaginary + other.imaginary)

    operator fun minus(other: Complex): Complex =
        Complex(real - other.real, imaginary - other.imaginary)

    operator fun times(other: Complex): Complex =
        Complex(
            (real * other.real) - (imaginary * other.imaginary),
            (real * other.imaginary) + (imaginary * other.real)
        )

    operator fun div(other: Complex): Complex = this * other.reciprocal()

    fun pow(complex: Complex): Complex {
        val arg = atan2(this.imaginary, this.real)
        val resultAbsolute = exp(ln(this.abs()) * complex.real - (arg * complex.imaginary))
        val resultArg = ln(this.abs()) * complex.imaginary + arg * complex.real
        return fromPolarCoord(resultAbsolute, resultArg)
    }

    override fun equals(other: Any?) = other != null && other is Complex && real == other.real && imaginary == other.imaginary

    companion object {
        fun fromPolarCoord(absolute: Double, arg: Double): Complex {
            return Complex(cos(arg) * absolute, sin(arg) * absolute)
        }

        val ZERO = Complex(0.0, 0.0)
    }
}

operator fun Double.plus(complex: Complex) = Complex(this, 0.0) + complex
operator fun Double.times(complex: Complex) = Complex(this, 0.0) * complex
operator fun Double.minus(complex: Complex) = Complex(this, 0.0) - complex
operator fun Double.div(complex: Complex) = Complex(this, 0.0) / complex

fun Double.toComplex() = Complex(this, 0.0)
fun Double.pow(complex: Complex) = Complex(this, 0.0).pow(complex)
