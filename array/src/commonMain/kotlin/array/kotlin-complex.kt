package array.complex

import kotlin.math.hypot

data class Complex(val real: Double, val imaginary: Double) {

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

    companion object {
        val ZERO = Complex(0.0, 0.0)
    }
}

operator fun Double.plus(complex: Complex) = Complex(this, 0.0) + complex
operator fun Double.times(complex: Complex) = Complex(this, 0.0) * complex
operator fun Double.minus(complex: Complex) = Complex(this, 0.0) - complex
operator fun Double.div(complex: Complex) = Complex(this, 0.0) / complex
fun Double.toComplex() = Complex(this, 0.0)
