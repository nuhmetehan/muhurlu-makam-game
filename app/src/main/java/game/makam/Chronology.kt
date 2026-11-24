package game.makam

private val quarterMonths = listOf("Ocak", "Nisan", "Temmuz", "Ekim")

fun gameDate(turn: Int): String {
    require(turn >= 0)
    return "${quarterMonths[turn % 4]} ${START_YEAR + turn / 4}"
}

data class PresidentialPeriod(
    val number: Int,
    val president: President,
    val startTurn: Int,
    val endTurn: Int,
    val completed: Boolean,
    val nickname: String? = null,
    val ending: Ending? = null
) {
    val dates get() = "${gameDate(startTurn)} — ${if (completed) gameDate(endTurn) else "görevde"}"
    val duration get() = tenure(endTurn - startTurn)
}

fun GameState.presidentialPeriods(): List<PresidentialPeriod> = history.mapIndexed { index, legacy ->
    PresidentialPeriod(index + 1, legacy.president, legacy.startTurn, legacy.startTurn + legacy.quarters,
        completed = true, nickname = legacy.nickname, ending = legacy.ending)
} + if (inSuccession) emptyList() else listOf(
    PresidentialPeriod(history.size + 1, president, turn - quarters, turn, completed = false)
)
