package com.amigocloud.amigosurvey.util

import android.databinding.BindingAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide

object DataBindingAdapters {

    @JvmStatic
    @BindingAdapter("srcCompat")
    fun setImageSrc(view: ImageView, url: String?) {
        if (url != null) Glide.with(view).load(url).into(view)
        else view.setImageBitmap(null)
    }

}
