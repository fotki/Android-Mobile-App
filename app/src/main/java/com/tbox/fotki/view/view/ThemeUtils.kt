package com.tbox.fotki.view.view

import android.app.Activity
import android.content.res.Configuration
import com.tbox.fotki.R
import com.tbox.fotki.refactoring.screens.MainActivity

class ThemeUtils {

    companion object ThemeUtils {
        fun setThemeDark(activity: MainActivity) {
            activity.setTheme(R.style.darkTheme)
        }

        fun setThemeLight(activity: MainActivity) {
           // activity.setTheme(R.style.lighTheme)
        }
    }

    fun isDarkTheme(activity: Activity): Boolean {
        return activity.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

}