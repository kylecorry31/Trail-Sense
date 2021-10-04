package com.kylecorry.trail_sense.weather.domain.clouds

// https://www.metoffice.gov.uk/binaries/content/assets/metofficegovuk/pdf/research/library-and-archive/library/publications/factsheets/factsheet_1-clouds.pdf
enum class Precipitation {
    Rain,
    Drizzle,
    Snow,
    SnowPellets,
    Hail,
    SmallHail,
    IcePellets,
    SnowGrains,
    Lightning
}