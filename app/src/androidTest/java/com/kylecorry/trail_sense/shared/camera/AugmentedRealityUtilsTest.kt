package com.kylecorry.trail_sense.shared.camera

import android.opengl.Matrix
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kylecorry.sol.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

class AugmentedRealityUtilsTest {

    @Test
    fun arToEnuUndoesCameraAxisSwapWithIdentityRotation() {
        val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

        val result = AugmentedRealityUtils.arToEnu(Vector3(0f, 0f, 1f), identity)

        assertVectorEquals(Vector3(0f, 1f, 0f), result)
    }

    @Test
    fun arToEnuAndEnuToArAreInversesWithRotation() {
        val rotation = FloatArray(16).also { Matrix.setRotateM(it, 0, 37f, 1f, 2f, 3f) }
        val enu = Vector3(3f, -5f, 7f)
        val ar = Vector3(-2f, 4f, 6f)

        assertVectorEquals(
            enu,
            AugmentedRealityUtils.arToEnu(AugmentedRealityUtils.enuToAr(enu, rotation), rotation)
        )
        assertVectorEquals(
            ar,
            AugmentedRealityUtils.enuToAr(AugmentedRealityUtils.arToEnu(ar, rotation), rotation)
        )
    }

    private fun assertVectorEquals(expected: Vector3, actual: Vector3) {
        assertEquals(expected.x, actual.x, 0.0001f)
        assertEquals(expected.y, actual.y, 0.0001f)
        assertEquals(expected.z, actual.z, 0.0001f)
    }
}
