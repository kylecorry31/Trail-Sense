package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.sol.math.SolMath.square
import kotlin.math.abs
import kotlin.math.log2

class GLCMService {

    fun features(glcm: GLCM): GLCMFeatures {
        var energy = 0f
        var entropy = 0f
        var contrast = 0f
        var homogeneity = 0f
        for (a in glcm.indices) {
            for (b in glcm[0].indices) {
                val p = glcm[a][b]
                energy += square(p)
                if (p > 0) {
                    entropy += -p * log2(p)
                }
                contrast += square((a - b).toFloat()) * p
                homogeneity += p / (1 + abs(a - b))
            }
        }
        return GLCMFeatures(energy, entropy, contrast, homogeneity)
    }

    fun energy(glcm: GLCM): Float {
        return features(glcm).energy
    }

    fun entropy(glcm: GLCM): Float {
        return features(glcm).entropy
    }

    fun contrast(glcm: GLCM): Float {
        return features(glcm).contrast
    }

    fun homogeneity(glcm: GLCM): Float {
        return features(glcm).homogeneity
    }

}