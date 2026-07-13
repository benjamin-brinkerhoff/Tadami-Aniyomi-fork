package eu.kanade.domain.easteregg.aurora

// СГЕНЕРИРОВАНО tools/aurora_forge.mjs — не редактировать вручную.
// В этом файле НЕТ секрета: только соли, контрольные хеши ключей (PBKDF2,
// 120000 итераций) и AES-256-GCM-шифртекст. Ответы и награда в кодовой
// базе не существуют — их невозможно извлечь анализом кода.
object AuroraVaultData {

    // Версия ваулта для мягкой миграции прогресса (AuroraHeartManager).
    const val VERSION = 64936285

    const val FIRST_RIDDLE = "Аврора шепчет тем, кто не спит: назови время, " +
        "когда граница между небом и сном тоньше всего. Ответ отдай поиску."

    val STAGES = listOf(
        AuroraStage(
            salt = "Zz0Vnfqo8t0XAnxL8epwaQ==",
            iv = "kW4HXNx83OiVQPZA",
            check = "Qmv0NtYSY2j19EQdpFNZUB2ZDTswqy+LrgjS6uBPyCk=",
            data = "VErxAs3KVJ1ozWFq6yQBiXVx1x3XzYIJASnd6CWHKgaOwVIqZIiflXl7rV7RqvLctR5Gh+QsPfh/uGrL " +
                "5cSMgJJYy+VpPTd36RELFxc3TCkVj8CCjp2S7bIg2byqMq4Y95p1OSfflN/wEYV+56rylJQz1lIhDdQi " +
                "vzvJOoa0JA13OTfLQBq/rTSjVPMPx+/L+jTlc9UVau5D/JC8eM0toUIR+NeWBcAQQd3CH4Ov1I6ox0pw " +
                "y9FzXRgDAaR8FnTJrheYTc3XjtJhZA4OFsAsm4to9w7/35fPwlSR3UznXQLQ+yzIqX6ahOexKSLjkw+d " +
                "Ez8h/SBf+cwviZX0SFml0ovJeyrM9BewnRJFTxRkZ6k41EoMoUTTmYycshEBAwiqlVhf21MQjA+vtJEn " +
                "anIMfx7/voHWCMzjCDq1XFcBMSsLjgSQ1JJ2cmQFNiE+QuWyJyPQPwxSP0wxsD8ak6hC3nVIyc7Abq8p " +
                "Net+C8uRyO5A2C6eTcGe5hIst6rr03x6P7NwCpcrQ616/Q0LUdgkhgoYNkKQGJL/i287ETKkaups1yeL " +
                "HZsNUs3iOSoN6RJF8lG4FyNn4ixSNEgd",
        ),
        AuroraStage(
            salt = "fIlXhLb/i/hxxbAqy8IwSg==",
            iv = "ce9X9xxMqvFpey0J",
            check = "GiwMCzIz2CXBQAiCYuK/jBLGZO+oa3JW0v2Ij/AD8vA=",
            data = "1y2Tbn5q1/WNvq3wjQTLYBTKhft2OSDvNod4fS7dchzijzm7k0uHVPsb1o2xMZwcmwWgMdj7XZ8ZRxjM " +
                "QBvcMcBWe/b73/bQzsJzamgjYm/GnC/gIK0KCsNcRyMJBm+uJFqGGWrs5slm6oWVox2EOzA2J29bmoXh " +
                "Am//lpZShPnHR81KjZH/WiJaEeHe85EpPfTDYxCeDXm2ymp/951PhngA9kDT2p5YEHX1ZR5/2fpIg2C5 " +
                "7MHkLvtIUr3sfgU3LQG2fNUfPfjMtHeyegvtN3uxCVO44jj7+QaiD0np1VAmWprJw25X8c1m6WP7T3Sr " +
                "JYiibzkBTA8ktAWlxPJoM7JWKq/VuOjxO5OjIa/qAn9Z4wIfAlGMzMrG7N+v9PGp6CIv89e6nx5s7NS0 " +
                "xGFDHh637MAIu41g/np06P9Q0CoIYgTBVEeSNBDDBQ5LyDN6ezc7MwYg94pnD+T0C9tAlba7THaxAJvs " +
                "F5/WQgTBq5Y22FwkzeOaW4ZyM9wkyzW24SbyYFjm451wcH7uBuNbFKHHqYcPsvpoDqXmherRSO/rY9da " +
                "m9mnXIyzF1tcr1qXTlzQMdVmlJrW17kWG/DPpq+Y+w==",
        ),
        AuroraStage(
            salt = "YG39LqO4rlhVGvvAqOiy8A==",
            iv = "xtslQAez/6rfyZSj",
            check = "q6l5dXkmRUREGiXEeGDPTXBVjGNLec2zwpm7LjL0mhA=",
            data = "o1m4135OLWIhVdMBgzyin53opHS2rIQtqEz4ASuxyhhI9sm8fkDGZiqQS5XrZATTHaOlk9w8ZoJuDVsD " +
                "76W/i1y4r91RWPp0uILCv668rwyfewDViNbU43xgYzU1SI7G7QT6OhuBGvwhoVW6nkYaTTnQU2FBgRWl " +
                "Wh2Q0ZSkzxFQpGv4Cuup1dqc3K+zqOBDjAFITA8+aEu6ukNQkrIaBTudd2sS/kR5NB7Ep/vYh4jixdlc " +
                "nMQm2ADO6GMv2NeVkQc329mxEqZdcX9SwZxsF+Y7M0Et//dlJIxzxOHbECJxQhD7MvSWM+yoDqw1Yc34 " +
                "zEAfdbloMWZr++J9LfUYsSYL/qrBwgapgPRlaqNwr/cfwwDux+4nXtotl6NrlhhkhEzv3sN08vHfyjVR " +
                "DhbmO2uG9FmhfEQ1CocXLJZSQOnqLL2PCezh4dLmCIP2ZlL0MRc4IUr08iC6axPlt92G3fEsIhBNN32I " +
                "v0jbwXs4dDtY5/GAhwTkPT537xNwPPRsmNtiAnIEN189lwuyFn2IP+OYI3V9AYpnoerv1P3HE5v54qEH " +
                "VeYqThyMhsu6heCFpnO/PwU0o54UtBdRlU0oXKsYdk0h9114+ZjdyisYTKW/jY9z1eIaqZLceq5bkNP3 " +
                "d7kDKNTfcjPKcA5nUXAu3/k5cZT25M3mc0hUAEAl+eaqEzV6ZCo03FZE/hwCdRpryrjePupkWe6XvSSS " +
                "/CfwKgVQ8YVS5c+x3RMowe6IriaJoeAuHFW/coiY7Jos1ToVbtI1/wtLlpuwh5tSlZck4R/OKdaK8+E2 " +
                "Q+9EmJ9Y2DaegnoZCBaV4gFkA31yPcOMMqwyNBxm5MbBKno2Ca3rdgEtDhZTKOIroE6EL0LV8+suWd04 " +
                "9phba33j8pU+uf1m1f+8hqL1ljOT931HsN3DmyKFXzzp2NiIPRYVRftablz/LI6LcP1qZIlh5WVZvYpF " +
                "GdyARVYGpIxsWqlOtHSr85c++M8wok3/Zy30958W69ftqKA6hUnqRqBJSUbT9meCeJXDCaVTHhfKHjol " +
                "qp3f/Ax111SsGfz7zc/QYrAK3Pa5E9/QhM8d59bEe7Xn4JyrO8fdtSsdLY3ylKuBLJiPBAce8wNyXN4t " +
                "ecBJowgDTFXs7lxfvwlKjwb0DX6kb9Zokq/u44dn+zYSu/nOmoHCpGYiQVTx60juy0MvC648QG+FPPkv " +
                "+9abJxgUekt4bqM9eVzxPWRJbwWr4xroVeN1HqJFP/DhDB34AmryKbD2BmHsRnaVAdtmoWi61DFewPaR " +
                "ZlmCfqR6tPo+CpkJjyvIsRF5+vEbLeQDnLZgKHv2nvUMDBwcZMVHmclL4R10g6gz5Ks0ZOjPMYfl8diZ " +
                "RU4eXsQctx+x8sBL",
        ),
    )
}
