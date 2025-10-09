package com.example.facedetectionar

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder

/**
 * MediaPipe Face Landmarker wrapper used ONLY to refresh landmarks
 * for logFaceToCameraAngles(). It does not drive placement/anchors.
 */
class AnglesOnlyLandmarker(
    private val context: Context,
    private val maxFaces: Int = 1,
    private val runningMode: RunningMode = RunningMode.IMAGE,
) {

    private var landmarker: FaceLandmarker? = null
    private var _latestLandmarks: List<NormalizedLandmark> = emptyList()
    val latestLandmarks: List<NormalizedLandmark> get() = _latestLandmarks

    fun start(modelAssetPath: String = "face_landmarker.task") {
        if (landmarker != null) return

        val opts = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                com.google.mediapipe.tasks.core.BaseOptions.builder()
                    .setModelAssetPath(modelAssetPath)
                    .build()
            )
            .setRunningMode(runningMode)
            .setNumFaces(maxFaces)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()

        landmarker = FaceLandmarker.createFromOptions(context, opts)
    }

    fun close() {
        landmarker?.close()
        landmarker = null
        _latestLandmarks = emptyList()
    }

    /**
     * Run MediaPipe on a single frame (bitmap should already be upright for model).
     * This ONLY updates latest landmarks; no placement logic here.
     */
    fun process(bitmap: Bitmap, rotationDegrees: Int = 0) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val imageOpts = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()
        val result: FaceLandmarkerResult? = landmarker?.detect(mpImage, imageOpts)
        _latestLandmarks = if (result != null && result.faceLandmarks().isNotEmpty())
            result.faceLandmarks()[0]
        else
            emptyList()
    }

    fun hasLandmarks(): Boolean = _latestLandmarks.isNotEmpty()
}
