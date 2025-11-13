package com.example.facedetectionar

import android.app.Dialog
import android.graphics.Paint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.jvm.java
import kotlin.text.toInt

class welcomeActivity : AppCompatActivity() {


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



}

