package health.openwater.openlifu3dscanner

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

private const val EVENT_LOGIN = "login"
private const val EVENT_LOGOUT = "logout"

private const val EVENT_OPEN_REVIEW_CAPTURES_SCREEN = "review_captures_screen_open"
private const val EVENT_REVIEW_CAPTURE = "review_capture"
private const val EVENT_DELETE_CAPTURE = "delete_capture"
private const val EVENT_OPEN_NEW_CAPTURE_SCREEN = "new_capture_screen_open"
private const val EVENT_START_CAPTURE = "start_capture"
private const val EVENT_FINISH_CAPTURE = "finish_capture"
private const val EVENT_DISCARD_CAPTURE = "discard_capture"

private const val EVENT_DOWNLOAD_PHOTOCOLLECTION = "download_photocollection"
private const val EVENT_DOWNLOAD_SCAN = "download_scan"

private const val EVENT_START_RECONSTRUCTION = "start_reconstruction"
private const val EVENT_RECONSTRUCTION_COMPLETE = "reconstruction_complete"
private const val EVENT_RECONSTRUCTION_FAIL = "reconstruction_fail"
private const val EVENT_NOT_ENOUGH_CREDITS = "not_enough_credits"

private const val PARAM_NUMBER_OF_PHOTOS = "number_of_photos"

object Analytics {

    fun onLogin() {
        logEvent(EVENT_LOGIN)
    }

    fun onLogout() {
        logEvent(EVENT_LOGOUT)
    }

    fun onAppStart() {
        logEvent(FirebaseAnalytics.Event.APP_OPEN)
    }

    fun onOpenReviewCapturesScreen() {
        logEvent(EVENT_OPEN_REVIEW_CAPTURES_SCREEN)
    }

    fun onReviewCapture() {
        logEvent(EVENT_REVIEW_CAPTURE)
    }

    fun onCaptureDeleted() {
        logEvent(EVENT_DELETE_CAPTURE)
    }

    fun onPhotocollectionDownloaded() {
        logEvent(EVENT_DOWNLOAD_PHOTOCOLLECTION)
    }

    fun onPhotoscanDownloaded() {
        logEvent(EVENT_DOWNLOAD_SCAN)
    }

    fun onNewCaptureScreenOpened() {
        logEvent(EVENT_OPEN_NEW_CAPTURE_SCREEN)
    }

    fun onCaptureStarted() {
        logEvent(EVENT_START_CAPTURE)
    }

    fun onCaptureFinished(numPhotos: Int) {
        logEvent(EVENT_FINISH_CAPTURE, bundleOf(PARAM_NUMBER_OF_PHOTOS to numPhotos))
    }

    fun onCaptureDiscarded() {
        logEvent(EVENT_DISCARD_CAPTURE)
    }

    fun onStartReconstruction() {
        logEvent(EVENT_START_RECONSTRUCTION)
    }

    fun onReconstructionComplete() {
        logEvent(EVENT_RECONSTRUCTION_COMPLETE)
    }

    fun onReconstructionFailed() {
        logEvent(EVENT_RECONSTRUCTION_FAIL)
    }

    fun onNotEnoughCredits() {
        logEvent(EVENT_NOT_ENOUGH_CREDITS)
    }

    private fun logEvent(event: String, bundle: Bundle? = null) {
        Firebase.analytics.logEvent(event, bundle)
    }

}