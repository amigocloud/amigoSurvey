package com.amigocloud.amigosurvey.util

import android.databinding.BindingAdapter
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

object DataBindingAdapters {

    @JvmStatic
    @BindingAdapter("srcCompat")
    fun setImageSrc(view: ImageView, url: String?) {
        if (url != null) Glide.with(view).load(url).into(view)
        else view.setImageBitmap(null)
    }

    @JvmStatic
    @BindingAdapter("android:visibility")
    fun setVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
