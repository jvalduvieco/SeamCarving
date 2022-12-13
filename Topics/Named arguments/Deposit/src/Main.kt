import kotlin.math.pow

fun calculate(startingAmount: Int = 1000, yearlyPercent: Double = 5.0, numberOfYears: Int = 10): Double {
    return startingAmount * (1 + yearlyPercent / 100.toDouble()).pow(numberOfYears)
}

fun main() {
    val parameter = readln()
    val value = readln()
    when (parameter) {
        "amount" -> println(calculate(startingAmount = value.toInt()).toInt())
        "percent" -> println(calculate(yearlyPercent = value.toDouble()).toInt())
        "years" -> println(calculate(numberOfYears = value.toInt()).toInt())
    }
}
