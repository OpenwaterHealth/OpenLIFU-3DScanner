package health.openwater.openlifu3dscanner.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import health.openwater.openlifu3dscanner.data.OrientationData
import kotlin.math.sqrt

class OrientationProvider(
    context: Context,
    private val onUpdate: (OrientationData) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVector =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val quat = FloatArray(4)

    private var started = false

    fun start() {
        if (!started) {
            sensorManager.registerListener(
                this,
                rotationVector,
                SensorManager.SENSOR_DELAY_GAME
            )
            started = true
        }
    }

    fun stop() {
        if (started) {
            sensorManager.unregisterListener(this)
            started = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        // Convert rotation vector → rotation matrix
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        // Convert rotation vector → quaternion (w,x,y,z)
        SensorManager.getQuaternionFromVector(quat, event.values)

        // Reorder to [x, y, z, w]
        val quaternion = floatArrayOf(quat[1], quat[2], quat[3], quat[0])

        // Extract camera axes
        val right = floatArrayOf(rotationMatrix[0], rotationMatrix[3], rotationMatrix[6])
        val up = floatArrayOf(rotationMatrix[1], rotationMatrix[4], rotationMatrix[7])
        val forward = floatArrayOf(-rotationMatrix[2], -rotationMatrix[5], -rotationMatrix[8])

        // Normalize vectors
        normalize(right)
        normalize(up)
        normalize(forward)

        onUpdate(
            OrientationData(
                quaternion = quaternion,
                rotationMatrix = rotationMatrix.clone(),
                forward = forward,
                up = up,
                right = right
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun normalize(v: FloatArray) {
        val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        if (len > 0.0001f) {
            v[0] /= len
            v[1] /= len
            v[2] /= len
        }
    }
}