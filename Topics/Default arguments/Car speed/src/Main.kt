fun checkSpeed(speed: Int, limit: Int = 60) {
    println(
        when (speed > limit) {
            true -> "Exceeds the limit by ${speed - limit} kilometers per hour"
            else -> "Within the limit"
        }
    )
}
