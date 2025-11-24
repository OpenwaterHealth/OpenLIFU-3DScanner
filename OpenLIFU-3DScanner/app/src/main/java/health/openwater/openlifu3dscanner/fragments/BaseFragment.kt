package health.openwater.openlifu3dscanner.fragments

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

open class BaseFragment: Fragment() {

    protected fun applyWindowInsets(view: View, displayCutout: Boolean = true) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val padding = insets.getInsets(
                when {
                    displayCutout -> WindowInsetsCompat.Type.displayCutout() or
                            WindowInsetsCompat.Type.systemBars()
                    else -> WindowInsetsCompat.Type.systemBars()
                }
            )
            v.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            insets
        }
    }

}