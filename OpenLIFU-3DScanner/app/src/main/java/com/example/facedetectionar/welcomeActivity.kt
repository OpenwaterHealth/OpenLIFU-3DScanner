package com.example.facedetectionar

import android.app.Dialog
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.facedetectionar.api.repository.UserRepository
import com.example.facedetectionar.dialogs.CloudStatusDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class welcomeActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        handleNoticeModal()

        subscribeToCloudStatus()








        val textViewDisclaimer=findViewById<TextView>(R.id.textViewDisclaimer);
        val newCaptureButton=findViewById<Button>(R.id.newCaptureButton);
        val reviewButton=findViewById<Button>(R.id.reviewButton);
        val logoClickImage=findViewById<ImageButton>(R.id.logoClickImage)
        val appVersionText=findViewById<TextView>(R.id.appVersionText)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
appVersionText.text="VER:${versionName}";
        textViewDisclaimer.paintFlags = textViewDisclaimer.paintFlags or Paint.UNDERLINE_TEXT_FLAG



        newCaptureButton.setOnClickListener {
            val intent=Intent(this,New_capture::class.java)
            startActivity(intent);
        }

        reviewButton.setOnClickListener {
            val intent = Intent(this, ReviewCaptures::class.java)
            startActivity(intent)
        }




        textViewDisclaimer.setOnClickListener {
            handleNoticeModal(forceShow = true)
        }


        logoClickImage.setOnClickListener {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.share_modal)
            dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.show()

        }


    }



    //  shows notice  dialog box
    fun handleNoticeModal(forceShow: Boolean = false) {
        val dialog = Dialog(this)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val doNotShow = prefs.getBoolean("do_not_show_notice", false)

        if (!forceShow && doNotShow) {
            return // Respect preference only if not forced
        }



        val view = layoutInflater.inflate(R.layout.modal_notice, null)
        val noticeAcknowledgeBtn = view.findViewById<Button>(R.id.noticeAcknowledgeBtn)
        val noticeCheckBox = view.findViewById<CheckBox>(R.id.noticeCheckBox)
        val doNotShowContainer = view.findViewById<LinearLayout>(R.id.doNotShowContainer)

        // If opened manually, hide the checkbox
        if (forceShow) {
            doNotShowContainer.visibility = View.GONE
        }

        dialog.setContentView(view)
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val marginInPx = (20 * metrics.density).toInt()
        val dialogWidth = screenWidth - (marginInPx * 2)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()

        noticeAcknowledgeBtn.setOnClickListener {
            if (noticeCheckBox.isChecked) {
                prefs.edit().putBoolean("do_not_show_notice", true).apply()
            }
            dialog.dismiss()
        }
    }

    private fun subscribeToCloudStatus() {
        val cloudStatusButton = findViewById<ImageView>(R.id.cloudStatusButton)
        val cloudStatusText = findViewById<TextView>(R.id.textCloudStatus)

        cloudStatusButton.setOnClickListener {
            val dialog = CloudStatusDialog()
            dialog.show(supportFragmentManager, CloudStatusDialog::class.simpleName)
        }

        lifecycleScope.launch {
            userRepository.getUserInfo().flowWithLifecycle(lifecycle).collect { userInfo ->
                cloudStatusButton.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this@welcomeActivity,
                        when {
                            userInfo != null -> R.drawable.ic_cloud_connected
                            else -> R.drawable.ic_cloud_not_connected
                        }
                    )
                )

                cloudStatusText.text = when {
                    userInfo != null -> userInfo.displayName
                    userRepository.authService.isSignedIn() -> getString(R.string.not_connected)
                    else -> getString(R.string.not_logged_in)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                userRepository.checkCloudAvailability()
                userRepository.refreshUserInfo()
            }
        }
    }

}

