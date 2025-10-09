package com.example.facedetectionar
// HeadPoseSolver.kt

import android.util.Log
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import kotlin.math.*

// ---- 1) Landmark set & canonical 3D model ----
private data class LM(val idx: Int, val name: String)

private val LM_SET = listOf(
    LM(1,   "nose_tip"),
    LM(33,  "left_eye_outer"),
    LM(263, "right_eye_outer"),
    LM(61,  "mouth_left"),
    LM(291, "mouth_right"),
    LM(199, "chin"),
    LM(9,   "glabella")
)

// Same canonical model as Python (millimeters). +X right, +Y up, +Z out of face
private val MODEL_3D = mapOf(
    "nose_tip"        to doubleArrayOf(  0.0,   0.0,   0.0),
    "left_eye_outer"  to doubleArrayOf(-32.0,  35.0, -25.0),
    "right_eye_outer" to doubleArrayOf( 32.0,  35.0, -25.0),
    "mouth_left"      to doubleArrayOf(-28.0, -38.0, -20.0),
    "mouth_right"     to doubleArrayOf( 28.0, -38.0, -20.0),
    "chin"            to doubleArrayOf(  0.0, -70.0,  -5.0),
    "glabella"        to doubleArrayOf(  0.0,  18.0, -15.0)
)

data class Intrinsics(val fx: Double, val fy: Double, val cx: Double, val cy: Double)

fun guessPinholeIntrinsics(w: Int, h: Int, hfovDeg: Double = 60.0): Intrinsics {
    val hfov = Math.toRadians(hfovDeg)
    val fx = w / (2.0 * tan(hfov / 2.0))
    val fy = fx * (h.toDouble() / w.toDouble())
    val cx = w / 2.0
    val cy = h / 2.0
    return Intrinsics(fx, fy, cx, cy)
}

// ---- 2) Euler from rotation matrix (OpenCV camera frame: +X right, +Y down, +Z forward) ----
data class YPR(val yawDeg: Float, val pitchDeg: Float, val rollDeg: Float)

fun rotationMatrixToYPR(R: Mat): YPR {
    // R is 3x3 double
    val r = DoubleArray(9); R.get(0,0,r)
    val r00 = r[0]; val r01 = r[1]; val r02 = r[2]
    val r10 = r[3]; val r11 = r[4]; val r12 = r[5]
    val r20 = r[6]; val r21 = r[7]; val r22 = r[8]

    val fx = r02; val fy = r12; val fz = r22 // forward = column 3
    val yaw   = Math.toDegrees(atan2(fx, fz))
    val pitch = Math.toDegrees(-atan2(fy, sqrt(fx*fx + fz*fz)))
    val ux = r01; val uy = r11 // up = column 2
    val roll  = Math.toDegrees(atan2(ux, uy))

    return YPR(yaw.toFloat(), pitch.toFloat(), roll.toFloat())
}

data class HeadPoseResult(
    val success: Boolean,
    val yprHeadInCam: YPR? = null,
    val rvec: Mat? = null,
    val tvec: Mat? = null,
    val cameraMatrix: Mat? = null,
    val distCoeffs: Mat? = null,
    val R_h2c: Mat? = null // rotation head->camera
)

// ---- 3) Build 2D/3D correspondences from ML Kit FaceMesh ----
fun computeHeadPoseFromFaceMesh(
    faceMesh: FaceMesh?,
    imageWidth: Int,
    imageHeight: Int,
    intr: Intrinsics
): HeadPoseResult {
    Log.i("OPENCV","LINE7.1")
    // index -> FaceMeshPoint
    val byIndex: Map<Int, FaceMeshPoint> = faceMesh!!.allPoints.associateBy{ it.index }
    Log.i("OPENCV","LINE7.2")
    val pts2D = ArrayList<Point>(LM_SET.size)
    val pts3D = ArrayList<Point3>(LM_SET.size)
    Log.i("OPENCV","LINE8")
    for (lm in LM_SET) {
        val p = byIndex[lm.idx]?.position ?: return HeadPoseResult(false)
        // MLKit FaceMesh pos is in px (x right, y down)
        pts2D += Point(p.x.toDouble(), p.y.toDouble())
        val m = MODEL_3D[lm.name] ?: return HeadPoseResult(false)
        pts3D += Point3(m[0], m[1], m[2])
    }
    Log.i("OPENCV","LINE9")
    val objPts = MatOfPoint3f(*pts3D.toTypedArray())
    val imgPts = MatOfPoint2f(*pts2D.toTypedArray())

    val cameraMatrix = Mat(3,3, CvType.CV_64F).apply {
        put(0,0, intr.fx, 0.0, intr.cx,
            0.0, intr.fy, intr.cy,
            0.0, 0.0, 1.0)
    }
//    val distCoeffs = Mat.zeros(4,1, CvType.CV_64F)
    val distCoeffs = MatOfDouble(*DoubleArray(4) { 0.0 })
    Log.i("OPENCV","LINE10")
    val rvec = Mat()
    val tvec = Mat()
    var ok = Calib3d.solvePnP(objPts, imgPts, cameraMatrix,
        distCoeffs, rvec, tvec, false, Calib3d.SOLVEPNP_ITERATIVE)
    Log.i("OPENCV","LINE10")
    if (!ok) {
        ok = Calib3d.solvePnP(objPts, imgPts, cameraMatrix, distCoeffs, rvec, tvec, false, Calib3d.SOLVEPNP_EPNP)
        if (!ok) return HeadPoseResult(false)
    }

    Log.i("OPENCV","LINE11")
    val R = Mat(); Calib3d.Rodrigues(rvec, R)
    val ypr = rotationMatrixToYPR(R)

    return HeadPoseResult(
        success = true,
        yprHeadInCam = ypr,
        rvec = rvec,
        tvec = tvec,
        cameraMatrix = cameraMatrix,
        distCoeffs = distCoeffs,
        R_h2c = R
    )
}

// ---- 4) Camera in head coordinates & angle between orientations ----
data class HeadCameraDual(
    val headInCam: YPR,
    val camInHead: YPR,
    val headForwardInCam: DoubleArray, // length 3, not normalized
    val camForwardInHead: DoubleArray, // length 3, not normalized
    val angleBetweenOrientationsDeg: Float
)

fun angleBetweenOrientations(R1: Mat, R2: Mat): Float {
    val Rt = Mat(); Core.gemm(R2, R1.t(), 1.0, Mat(), 0.0, Rt)
    val tr = Rt.get(0,0)[0] + Rt.get(1,1)[0] + Rt.get(2,2)[0]
    var cosang = (tr - 1.0) / 2.0
    cosang = cosang.coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(cosang)).toFloat()
}

fun headAndCameraYpr(R_h2c: Mat): HeadCameraDual {
    val headInCam = rotationMatrixToYPR(R_h2c)

    // Inverse rotation (camera->head) is transpose
    val R_c2h = R_h2c.t()
    val camInHead = rotationMatrixToYPR(R_c2h)

    // forward vectors = 3rd column
    val headForwardInCam = doubleArrayOf(R_h2c.get(0,2)[0], R_h2c.get(1,2)[0], R_h2c.get(2,2)[0])
    val camForwardInHead = doubleArrayOf(R_c2h.get(0,2)[0], R_c2h.get(1,2)[0], R_c2h.get(2,2)[0])

    val ang = angleBetweenOrientations(Mat.eye(3,3,CvType.CV_64F), R_h2c)
    return HeadCameraDual(headInCam, camInHead, headForwardInCam, camForwardInHead, ang)
}


/** Nose in camera coordinates.
 *  Returns in BOTH millimetres and meters for convenience. */
data class NoseCam(
    val x_mm: Double, val y_mm: Double, val z_mm: Double,
    val x_m: Double,  val y_m: Double,  val z_m: Double
)

/** From your PnP result: since nose model = (0,0,0), nose_cam = tvec. */
fun noseInCamera(pose: HeadPoseResult): NoseCam? {
    val t = pose.tvec ?: return null
    // t is 3x1 CV_64F
    val xmm = t.get(0,0)[0]
    val ymm = t.get(1,0)[0]
    val zmm = t.get(2,0)[0]
    return NoseCam(
        x_mm = xmm, y_mm = ymm, z_mm = zmm,
        x_m = xmm / 1000.0, y_m = ymm / 1000.0, z_m = zmm / 1000.0
    )
}


fun cameraToNoseDistanceM(pose: HeadPoseResult): Double? {
    val t = pose.tvec ?: return null              // 3x1 CV_64F (mm)
    val distMm = org.opencv.core.Core.norm(t)     // Euclidean norm in millimetres
    return distMm / 1000.0                        // convert to meters
}

