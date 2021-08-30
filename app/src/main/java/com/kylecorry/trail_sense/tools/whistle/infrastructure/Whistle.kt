package com.kylecorry.trail_sense.tools.whistle.infrastructure

import com.kylecorry.andromeda.sound.SoundPlayer
import com.kylecorry.andromeda.sound.ToneGenerator

class Whistle: SoundPlayer(ToneGenerator().getTone(3150))