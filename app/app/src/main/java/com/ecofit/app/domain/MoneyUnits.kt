package com.ecofit.app.domain

// 금액을 생활 단위로 바꾼다. 큰 단위부터 보고 1개 이상 되는 걸 고른다
object MoneyUnits {
    private class Unit(val won: Int, val name: String, val counter: String)

    private val units = listOf(
        Unit(20_000, "치킨", "마리"),
        Unit(9_000, "국밥", "그릇"),
        Unit(4_500, "커피", "잔"),
        Unit(1_500, "삼각김밥", "개"),
    )

    fun describe(won: Int): String? {
        if (won < units.last().won) return null
        val u = units.first { won >= it.won }
        val n = won / u.won
        return "${u.name} ${n}${u.counter} 값"
    }

    // 아직 1단위가 안 될 때, 가장 작은 단위까지 얼마 남았는지
    fun untilNext(won: Int): String {
        val u = units.last()
        return "${u.name} 1${u.counter}까지 ${"%,d".format(u.won - won)}원"
    }
}
