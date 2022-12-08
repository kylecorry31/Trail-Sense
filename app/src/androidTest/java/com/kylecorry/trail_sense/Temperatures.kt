package com.kylecorry.trail_sense

object Temperatures {
    val canadaLow = arrayOf(-31.0f, -27.6f, -21.7f, -10.2f, -1.5f, 4.6f, 8.4f, 6.6f, 1.6f, -4.2f, -15.8f, -26.8f)
    val canadaHigh = arrayOf(-20.8f, -15.2f, -7.0f, 2.9f, 11.6f, 18.1f, 21.0f, 19.9f, 12.4f, 4.3f, -7.5f, -17.1f)

    val alaskaLow = arrayOf(-31.4f, -27.9f, -24.5f, -14.7f, -3.8f, 2.9f, 4.8f, 2.2f, -3.7f, -12.7f, -23.4f, -28.9f)
    val alaskaHigh = arrayOf(-22.8f, -18.4f, -12.3f, -1.4f, 8.8f, 17.0f, 18.2f, 14.7f, 7.0f, -5.2f, -15.8f, -20.9f)

    val californiaLow = arrayOf(2.6f, 4.1f, 5.4f, 6.2f, 8.4f, 10.6f, 12.4f, 12.9f, 11.7f, 9.1f, 5.3f, 2.5f)
    val californiaHigh = arrayOf(13.8f, 15.9f, 17.3f, 20.3f, 23.1f, 26.1f, 28.8f, 29.4f, 28.3f, 25.3f, 19.1f, 14.6f)

    val mexicoLow = arrayOf(-5.8f, -4.2f, -1.4f, 2.5f, 6.2f, 8.3f, 8.5f, 8.4f, 6.8f, 3.1f, -1.2f, -4.9f)
    val mexicoHigh = arrayOf(10.6f, 13.1f, 17.0f, 20.8f, 23.9f, 25.0f, 24.3f, 23.8f, 22.0f, 18.8f, 15.3f, 10.7f)

    val costaRicaLow = arrayOf(17.1f, 17.3f, 18.2f, 19.0f, 18.8f, 18.3f, 18.6f, 18.2f, 17.7f, 17.5f, 17.4f, 17.0f)
    val costaRicaHigh = arrayOf(25.9f, 26.4f, 27.5f, 27.7f, 27.3f, 26.4f, 26.4f, 26.4f, 26.2f, 26.0f, 25.3f, 25.3f)

    val puertoRicoLow = arrayOf(13.6f, 13.5f, 13.8f, 14.7f, 15.9f, 16.5f, 16.6f, 16.8f, 16.7f, 16.3f, 15.7f, 14.5f)
    val puertoRicoHigh = arrayOf(26.0f, 26.1f, 26.7f, 27.3f, 27.8f, 28.5f, 28.8f, 28.9f, 28.8f, 28.4f, 27.3f, 26.2f)

    val newYorkLow = arrayOf(-9.5f, -9.8f, -6.8f, -1.6f, 4.0f, 10.0f, 13.5f, 13.3f, 9.8f, 4.7f, 0.0f, -5.3f)
    val newYorkHigh = arrayOf(-2.2f, -1.9f, 1.7f, 7.1f, 14.0f, 19.7f, 22.8f, 21.9f, 18.0f, 12.6f, 6.6f, 0.9f)

    val greenlandLow = arrayOf(-25.4f, -25.8f, -25.5f, -22.4f, -16.9f, -12.3f, -10.9f, -11.7f, -15.6f, -20.3f, -23.4f, -24.4f)
    val greenlandHigh = arrayOf(-21.1f, -21.4f, -20.8f, -18.1f, -12.7f, -9.2f, -7.7f, -8.6f, -12.6f, -17.2f, -19.4f, -20.3f)

    val hawaiiLow = arrayOf(-9.1f, -9.0f, -8.5f, -7.5f, -6.5f, -5.4f, -4.9f, -4.9f, -5.2f, -5.7f, -6.7f, -8.2f)
    val hawaiiHigh = arrayOf(2.7f, 2.7f, 2.8f, 3.4f, 4.6f, 5.6f, 6.1f, 6.3f, 5.9f, 5.4f, 4.1f, 2.9f)

    val equadorLow = arrayOf(5.9f, 6.1f, 6.2f, 6.4f, 6.3f, 5.7f, 5.5f, 5.4f, 5.6f, 5.9f, 5.9f, 5.9f)
    val equadorHigh = arrayOf(15.5f, 15.4f, 15.5f, 15.4f, 15.4f, 14.7f, 14.4f, 14.9f, 15.8f, 16.1f, 16.3f, 15.9f)

    val brazilLow = arrayOf(21.2f, 21.0f, 21.2f, 21.3f, 20.9f, 20.0f, 19.1f, 19.7f, 20.5f, 21.3f, 21.2f, 21.3f)
    val brazilHigh = arrayOf(28.6f, 28.6f, 28.7f, 29.3f, 29.8f, 30.5f, 31.1f, 31.8f, 31.4f, 31.1f, 30.6f, 29.5f)

    val argentinaLow = arrayOf(15.6f, 14.6f, 12.8f, 9.1f, 6.1f, 3.0f, 2.0f, 3.1f, 5.6f, 9.3f, 12.1f, 14.7f)
    val argentinaHigh = arrayOf(28.4f, 27.2f, 24.7f, 21.8f, 18.9f, 15.3f, 15.2f, 17.7f, 20.2f, 23.3f, 25.6f, 28.0f)

    val boliviaLow = arrayOf(19.1f, 19.0f, 18.9f, 16.4f, 14.6f, 13.3f, 12.4f, 12.9f, 15.4f, 17.2f, 18.1f, 18.6f)
    val boliviaHigh = arrayOf(28.7f, 28.7f, 27.8f, 27.4f, 25.0f, 23.2f, 23.8f, 25.8f, 28.0f, 28.6f, 29.1f, 28.9f)

    val chileLow = arrayOf(0.3f, 0.0f, -1.2f, -2.9f, -4.9f, -6.2f, -6.7f, -6.2f, -4.7f, -3.0f, -1.9f, -0.4f)
    val chileHigh = arrayOf(8.3f, 7.9f, 6.5f, 4.2f, 1.3f, -0.5f, -0.7f, 0.0f, 1.9f, 4.5f, 5.9f, 7.5f)

}