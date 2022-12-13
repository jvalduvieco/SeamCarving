// complete this function
fun concatenate(first: String, second: String, third: String, separator: String = " "): String {
    val strings = listOf(first, second, third)
    return strings.joinToString(separator)
}
