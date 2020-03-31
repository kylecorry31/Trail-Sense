package com.kylecorry.trail_sense.astronomy.sun

class CivilTwilightCalculator : BaseSunTimesCalculator(-6f)
class NauticalTwilightCalculator : BaseSunTimesCalculator(-12f)
class AstronomicalTwilightCalculator : BaseSunTimesCalculator(-18f)
class ActualTwilightCalculator : BaseSunTimesCalculator(-0.833f)
