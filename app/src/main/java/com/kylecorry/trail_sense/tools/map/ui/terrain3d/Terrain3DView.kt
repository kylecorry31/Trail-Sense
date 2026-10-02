package com.kylecorry.trail_sense.tools.map.ui.terrain3d

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a [MapTerrain] in 3D. One finger orbits, two fingers pan, and pinch zooms.
 */
class Terrain3DView(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val renderer = TerrainRenderer()

    private val scaleDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.distance = (renderer.distance / detector.scaleFactor)
                    .coerceIn(renderer.extent * 0.1f, renderer.extent * 4f)
                requestRender()
                return true
            }
        })

    private var lastX = 0f
    private var lastY = 0f
    private var lastPointerCount = 0

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setTerrain(terrain: MapTerrain) {
        renderer.sun = terrain.sun
        renderer.moon = terrain.moon
        queueEvent { renderer.setMesh(terrain.mesh, terrain.texture) }
        requestRender()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val count = event.pointerCount
        val x = (0 until count).map { event.getX(it) }.average().toFloat()
        val y = (0 until count).map { event.getY(it) }.average().toFloat()

        if (event.actionMasked == MotionEvent.ACTION_MOVE && count == lastPointerCount) {
            val dx = x - lastX
            val dy = y - lastY
            if (count == 1) {
                renderer.yaw -= dx * 0.4f
                renderer.pitch = (renderer.pitch + dy * 0.3f).coerceIn(5f, 89f)
            } else {
                // Two fingers pan the target across the ground
                val yaw = Math.toRadians(renderer.yaw.toDouble())
                val k = renderer.distance * 0.002f
                val rightX = cos(yaw).toFloat()
                val rightZ = -sin(yaw).toFloat()
                val forwardX = -sin(yaw).toFloat()
                val forwardZ = -cos(yaw).toFloat()
                renderer.targetX += (-dx * rightX + dy * forwardX) * k
                renderer.targetZ += (-dx * rightZ + dy * forwardZ) * k
            }
            requestRender()
        }
        lastX = x
        lastY = y
        lastPointerCount = count
        return true
    }

    private class TerrainRenderer : Renderer {
        @Volatile
        var yaw = 20f

        @Volatile
        var pitch = 40f

        @Volatile
        var distance = 10f

        @Volatile
        var targetX = 0f

        @Volatile
        var targetZ = 0f

        @Volatile
        var extent = 10f

        @Volatile
        var sun = SkyBody(180f, 45f)

        @Volatile
        var moon = SkyBody(0f, -45f)

        private var mesh: TerrainMesh? = null
        private var targetY = 0f
        private var textureId = 0
        private var terrainProgram = 0
        private var pointProgram = 0
        private val projection = FloatArray(16)
        private val view = FloatArray(16)
        private val mvp = FloatArray(16)
        private var aspect = 1f

        fun setMesh(mesh: TerrainMesh, bitmap: Bitmap) {
            uploadTexture(bitmap)
            this.mesh = mesh
            extent = mesh.extent
            distance = mesh.extent * 1.2f
            targetX = 0f
            targetZ = 0f
            targetY = mesh.centerHeight
        }

        private fun uploadTexture(bitmap: Bitmap) {
            if (textureId == 0) {
                val ids = IntArray(1)
                GLES20.glGenTextures(1, ids, 0)
                textureId = ids[0]
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            // Non-power-of-two textures require clamping and no mipmaps in ES 2.0
            val target = GLES20.GL_TEXTURE_2D
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            terrainProgram = createProgram(TERRAIN_VERTEX, TERRAIN_FRAGMENT)
            pointProgram = createProgram(POINT_VERTEX, POINT_FRAGMENT)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            aspect = width.toFloat() / height
        }

        override fun onDrawFrame(gl: GL10?) {
            val sun = sun
            val moon = moon
            val daylight = ((sun.altitude + 6f) / 12f).coerceIn(0f, 1f)
            GLES20.glClearColor(
                0.02f + 0.51f * daylight,
                0.03f + 0.72f * daylight,
                0.08f + 0.87f * daylight,
                1f
            )
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            val mesh = mesh ?: return

            val pitchRad = Math.toRadians(pitch.toDouble())
            val yawRad = Math.toRadians(yaw.toDouble())
            val eyeX = targetX + distance * (cos(pitchRad) * sin(yawRad)).toFloat()
            val eyeY = targetY + distance * sin(pitchRad).toFloat()
            val eyeZ = targetZ + distance * (cos(pitchRad) * cos(yawRad)).toFloat()
            Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, targetX, targetY, targetZ, 0f, 1f, 0f)
            Matrix.perspectiveM(projection, 0, 50f, aspect, extent * 0.005f, extent * 50f)
            Matrix.multiplyMM(mvp, 0, projection, 0, view, 0)

            drawTerrain(mesh, sun, moon, daylight)

            val skyDistance = extent * 8f
            if (sun.altitude > -1f) {
                drawPoint(sun, skyDistance, 90f, floatArrayOf(1f, 0.92f, 0.5f, 1f))
            }
            if (moon.altitude > -1f) {
                drawPoint(moon, skyDistance, 60f, floatArrayOf(0.9f, 0.92f, 1f, 1f))
            }
        }

        private fun drawTerrain(mesh: TerrainMesh, sun: SkyBody, moon: SkyBody, daylight: Float) {
            GLES20.glUseProgram(terrainProgram)
            GLES20.glUniformMatrix4fv(uniform(terrainProgram, "uMVP"), 1, false, mvp, 0)

            val sunDir = direction(sun)
            val moonDir = direction(moon)
            val sunIntensity = (sun.altitude / 10f).coerceIn(0f, 1f)
            val moonIntensity = if (moon.altitude > 0f) 0.25f * (1f - daylight) else 0f
            GLES20.glUniform3f(uniform(terrainProgram, "uSunDir"), sunDir[0], sunDir[1], sunDir[2])
            GLES20.glUniform3f(
                uniform(terrainProgram, "uSunColor"),
                sunIntensity * 0.9f, sunIntensity * 0.85f, sunIntensity * 0.75f
            )
            GLES20.glUniform3f(uniform(terrainProgram, "uMoonDir"), moonDir[0], moonDir[1], moonDir[2])
            GLES20.glUniform3f(
                uniform(terrainProgram, "uMoonColor"),
                moonIntensity * 0.6f, moonIntensity * 0.7f, moonIntensity
            )
            val ambient = 0.12f + 0.28f * daylight
            GLES20.glUniform3f(uniform(terrainProgram, "uAmbient"), ambient, ambient, ambient)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(uniform(terrainProgram, "uTexture"), 0)

            val stride = 8 * 4
            val position = GLES20.glGetAttribLocation(terrainProgram, "aPosition")
            val normal = GLES20.glGetAttribLocation(terrainProgram, "aNormal")
            val uv = GLES20.glGetAttribLocation(terrainProgram, "aUv")
            val attributes = listOf(position to 3, normal to 3, uv to 2)
            var offset = 0
            for ((location, size) in attributes) {
                mesh.vertices.position(offset)
                GLES20.glVertexAttribPointer(
                    location, size, GLES20.GL_FLOAT, false, stride, mesh.vertices
                )
                GLES20.glEnableVertexAttribArray(location)
                offset += size
            }
            mesh.indices.position(0)
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                mesh.indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                mesh.indices
            )
            attributes.forEach { GLES20.glDisableVertexAttribArray(it.first) }
        }

        private fun drawPoint(body: SkyBody, distance: Float, size: Float, color: FloatArray) {
            val dir = direction(body)
            GLES20.glUseProgram(pointProgram)
            GLES20.glUniformMatrix4fv(uniform(pointProgram, "uMVP"), 1, false, mvp, 0)
            GLES20.glUniform3f(
                uniform(pointProgram, "uPosition"),
                dir[0] * distance, dir[1] * distance, dir[2] * distance
            )
            GLES20.glUniform1f(uniform(pointProgram, "uSize"), size)
            GLES20.glUniform4fv(uniform(pointProgram, "uColor"), 1, color, 0)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
        }

        // Scene space is x = east, y = up, z = south
        private fun direction(body: SkyBody): FloatArray {
            val az = Math.toRadians(body.azimuth.toDouble())
            val alt = Math.toRadians(body.altitude.toDouble())
            return floatArrayOf(
                (sin(az) * cos(alt)).toFloat(),
                sin(alt).toFloat(),
                (-cos(az) * cos(alt)).toFloat()
            )
        }

        private fun uniform(program: Int, name: String): Int {
            return GLES20.glGetUniformLocation(program, name)
        }

        private fun createProgram(vertex: String, fragment: String): Int {
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, compile(GLES20.GL_VERTEX_SHADER, vertex))
            GLES20.glAttachShader(program, compile(GLES20.GL_FRAGMENT_SHADER, fragment))
            GLES20.glLinkProgram(program)
            return program
        }

        private fun compile(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            return shader
        }
    }

    companion object {
        private const val TERRAIN_VERTEX = """
            uniform mat4 uMVP;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aUv;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vNormal = aNormal;
                vUv = aUv;
                gl_Position = uMVP * vec4(aPosition, 1.0);
            }
        """

        private const val TERRAIN_FRAGMENT = """
            precision mediump float;
            uniform vec3 uSunDir;
            uniform vec3 uSunColor;
            uniform vec3 uMoonDir;
            uniform vec3 uMoonColor;
            uniform vec3 uAmbient;
            uniform sampler2D uTexture;
            varying vec3 vNormal;
            varying vec2 vUv;
            void main() {
                vec3 n = normalize(vNormal);
                vec3 light = uAmbient
                    + uSunColor * max(dot(n, uSunDir), 0.0)
                    + uMoonColor * max(dot(n, uMoonDir), 0.0);
                // Keep the map readable even when it is dark
                vec3 shade = 0.5 + 0.7 * min(light, vec3(1.3));
                gl_FragColor = vec4(texture2D(uTexture, vUv).rgb * shade, 1.0);
            }
        """

        private const val POINT_VERTEX = """
            uniform mat4 uMVP;
            uniform vec3 uPosition;
            uniform float uSize;
            void main() {
                gl_Position = uMVP * vec4(uPosition, 1.0);
                gl_PointSize = uSize;
            }
        """

        private const val POINT_FRAGMENT = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                if (length(gl_PointCoord - vec2(0.5)) > 0.5) discard;
                gl_FragColor = uColor;
            }
        """
    }
}
