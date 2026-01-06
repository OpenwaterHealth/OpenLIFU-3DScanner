package health.openwater.openlifu3dscanner.screen.photoscan

import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

class ObjRenderer(
    private val modelDir: String
) : GLSurfaceView.Renderer {

    data class Bounds(
        val minX: Float, val maxX: Float,
        val minY: Float, val maxY: Float,
        val minZ: Float, val maxZ: Float
    ) {
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val centerZ = (minZ + maxZ) / 2f
        val width = maxX - minX
        val height = maxY - minY
        val depth = maxZ - minZ
    }

    private var vertexBuffer: FloatBuffer? = null
    private var uvBuffer: FloatBuffer? = null
    private var vertexCount = 0
    private var textureId = 0
    private var program = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var rotationX = 0f
    private var rotationY = 0f
    private var zoomFactor = 1f

    fun rotate(dx: Float, dy: Float) {
        rotationY += dx
        rotationX += dy
    }

    fun zoom(scaleFactor: Float) {
        val adjustedScale = 1f + (scaleFactor - 1f) * 0.5f  // Dampen zoom speed
        zoomFactor *= adjustedScale
        zoomFactor = zoomFactor.coerceIn(0.1f, 10f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1f)

        val result = try {
            parseObj(File(modelDir, "texturedMesh.obj"))
        } catch (_: FileNotFoundException) {
            return
        }

        vertexBuffer = result.vertices
        uvBuffer = result.uvs
        vertexCount = result.vertexCount
        textureId = loadTexture(File(modelDir, result.texture))

        // Calculate model bounds
        val bounds = calculateBounds(result.vertices, result.vertexCount)

        // Setup model matrix - center and scale the model
        Matrix.setIdentityM(modelMatrix, 0)

        // First scale, then translate (order matters!)
        val maxDim = max(max(bounds.width, bounds.height), bounds.depth)
        val scale = 1.6f / maxDim
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)

        // Translate the scaled model to origin
        Matrix.translateM(modelMatrix, 0, -bounds.centerX, -bounds.centerY, -bounds.centerZ)

        // Setup view matrix - camera further back to see more
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 5f,      // Camera position - moved from 3 to 5
            0f, 0f, 0f,      // Look at origin (model is centered there)
            0f, 1f, 0f       // Up vector
        )

        program = createProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (vertexBuffer == null) return
        if (uvBuffer == null) return

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Apply user rotations
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.rotateM(tempMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(tempMatrix, 0, rotationY, 0f, 1f, 0f)

        // Apply zoom by scaling
        val zoomedModel = FloatArray(16)
        Matrix.setIdentityM(zoomedModel, 0)
        Matrix.scaleM(zoomedModel, 0, zoomFactor, zoomFactor, zoomFactor)

        // Combine: MVP = Projection * View * Zoom * (Model * Rotation)
        val modelRotated = FloatArray(16)
        Matrix.multiplyMM(modelRotated, 0, modelMatrix, 0, tempMatrix, 0)
        Matrix.multiplyMM(modelRotated, 0, zoomedModel, 0, modelRotated, 0)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelRotated, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUseProgram(program)

        val pos = GLES20.glGetAttribLocation(program, "aPos")
        val uv = GLES20.glGetAttribLocation(program, "aUv")
        val mvpLoc = GLES20.glGetUniformLocation(program, "uMVP")

        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glEnableVertexAttribArray(uv)

        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(uv, 2, GLES20.GL_FLOAT, false, 0, uvBuffer)

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTex"), 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(uv)
    }


    private fun calculateBounds(buffer: FloatBuffer, vertexCount: Int): Bounds {
        buffer.position(0)

        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        for (i in 0 until vertexCount) {
            val x = buffer.get()
            val y = buffer.get()
            val z = buffer.get()

            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
            minZ = minOf(minZ, z)
            maxZ = maxOf(maxZ, z)
        }

        buffer.position(0)

        return Bounds(minX, maxX, minY, maxY, minZ, maxZ)
    }

    /* ===================== OBJ + MTL PARSER ===================== */

    private data class ObjResult(
        val vertices: FloatBuffer,
        val uvs: FloatBuffer,
        val vertexCount: Int,
        val texture: String
    )

    @Throws(FileNotFoundException::class)
    private fun parseObj(obj: File): ObjResult {
        val positions = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val vertices = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        var texture = ""

        obj.forEachLine { line ->
            when {
                line.startsWith("v ") -> {
                    val p = line.split(" ")
                    positions += p[1].toFloat()
                    positions += p[2].toFloat()
                    positions += p[3].toFloat()
                }

                line.startsWith("vt ") -> {
                    val t = line.split(" ")
                    texCoords += t[1].toFloat()
                    texCoords += 1f - t[2].toFloat()
                }

                line.startsWith("f ") -> {
                    val f = line.split(" ")
                    for (i in 1..3) {
                        val idx = f[i].split("/")
                        val vi = (idx[0].toInt() - 1) * 3
                        val ti = (idx[1].toInt() - 1) * 2
                        vertices += positions[vi]
                        vertices += positions[vi + 1]
                        vertices += positions[vi + 2]
                        uvs += texCoords[ti]
                        uvs += texCoords[ti + 1]
                    }
                }

                line.startsWith("mtllib ") -> {
                    val mtl = File(obj.parent, line.substringAfter("mtllib ").trim())
                    texture = mtl.readLines()
                        .first { it.startsWith("map_Kd ") }
                        .substringAfter("map_Kd ")
                }
            }
        }

        return ObjResult(
            vertices.toBuffer(),
            uvs.toBuffer(),
            vertices.size / 3,
            texture
        )
    }

    /* ===================== OPENGL HELPERS ===================== */

    private fun loadTexture(file: File): Int {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        return ids[0]
    }

    private fun List<Float>.toBuffer(): FloatBuffer =
        ByteBuffer.allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(toFloatArray())
            .apply { position(0) }

    private fun createProgram(): Int {
        val v = compile(
            GLES20.GL_VERTEX_SHADER,
            """
        attribute vec3 aPos;
        attribute vec2 aUv;
        uniform mat4 uMVP;
        varying vec2 vUv;
        void main() {
            gl_Position = uMVP * vec4(aPos, 1.0);
            vUv = aUv;
        }
        """
        )
        val f = compile(
            GLES20.GL_FRAGMENT_SHADER,
            """
        precision mediump float;
        varying vec2 vUv;
        uniform sampler2D uTex;
        void main() {
            gl_FragColor = texture2D(uTex, vUv);
        }
        """
        )

        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, v)
        GLES20.glAttachShader(prog, f)
        GLES20.glLinkProgram(prog)

        // Check for link errors
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(prog)
            GLES20.glDeleteProgram(prog)
            throw RuntimeException("Program link error: $error")
        }

        return prog
    }

    private fun compile(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)

        // Check for compile errors
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile error: $error")
        }

        return shader
    }
}