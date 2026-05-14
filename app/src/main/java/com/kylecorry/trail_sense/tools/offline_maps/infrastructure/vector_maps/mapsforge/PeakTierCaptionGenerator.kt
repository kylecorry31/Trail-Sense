package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

internal object PeakTierCaptionGenerator {

    const val PLACEHOLDER = "<trail_sense_peak_elevation_captions />"

    private data class Tier(
        val number: Int,
        val minElevationMeters: Float,
        val namePriority: Int,
        val elePriority: Int,
        val zoomMin: Int,
        val fontSize: Int
    )

    private val tiers = listOf(
        Tier(20, 8000f, 39, 29, zoomMin = 9, fontSize = 13),
        Tier(19, 7000f, 38, 28, zoomMin = 9, fontSize = 13),
        Tier(18, 6000f, 36, 26, zoomMin = 9, fontSize = 13),
        Tier(17, 5000f, 35, 25, zoomMin = 9, fontSize = 13),
        Tier(16, 4000f, 33, 23, zoomMin = 9, fontSize = 13),
        Tier(15, 3500f, 32, 22, zoomMin = 11, fontSize = 12),
        Tier(14, 3000f, 30, 20, zoomMin = 11, fontSize = 12),
        Tier(13, 2500f, 29, 19, zoomMin = 11, fontSize = 12),
        Tier(12, 2000f, 27, 17, zoomMin = 11, fontSize = 12),
        Tier(11, 1750f, 26, 16, zoomMin = 11, fontSize = 12),
        Tier(10, 1500f, 24, 14, zoomMin = 12, fontSize = 11),
        Tier(9, 1250f, 23, 13, zoomMin = 12, fontSize = 11),
        Tier(8, 1000f, 21, 11, zoomMin = 12, fontSize = 11),
        Tier(7, 750f, 20, 10, zoomMin = 12, fontSize = 11),
        Tier(6, 600f, 18, 8, zoomMin = 12, fontSize = 11),
        Tier(5, 500f, 17, 7, zoomMin = 14, fontSize = 11),
        Tier(4, 400f, 15, 5, zoomMin = 14, fontSize = 11),
        Tier(3, 300f, 14, 4, zoomMin = 14, fontSize = 11),
        Tier(2, 150f, 12, 2, zoomMin = 14, fontSize = 11),
        Tier(1, 0f, 11, 1, zoomMin = 14, fontSize = 11),
    )

    fun getElevationTier(elevationMeters: Float): Int {
        return tiers.firstOrNull { elevationMeters >= it.minElevationMeters }?.number ?: 1
    }

    fun generate(): String = tiers.joinToString("\n") { generateRule(it) }

    private fun generateRule(tier: Tier): String =
        """<rule e="any" k="trail_sense_elevation_tier" v="${tier.number}" zoom-min="${tier.zoomMin}">
                <caption fill="#000000" font-size="${tier.fontSize}" font-style="bold" k="name" position="above" priority="${tier.namePriority}" stroke="#FFFFFF" stroke-width="2.0" symbol-id="peak" display="order" />
                <rule e="any" k="*" v="*" zoom-min="${tier.zoomMin + 2}">
                    <caption fill="#000000" font-size="10" k="ele" position="below" priority="${tier.elePriority}" stroke="#FFFFFF" stroke-width="2.0" symbol-id="peak" />
                </rule>
            </rule>"""
}
