package com.amigocloud.amigosurvey.viewmodel

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import com.amigocloud.amigosurvey.repository.AmigoRest
import javax.inject.Inject

class LoginViewModel(private val rest: AmigoRest) : ViewModel() {

    val email = ObservableField<String>()
    val password = ObservableField<String>()

    fun login() = rest.login(email.get(), password.get())

    fun fetchUser() = rest.fetchUser()

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest) : ViewModelFactory<LoginViewModel>() {

        override val modelClass = LoginViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return LoginViewModel(rest) as T
            }
            throw IllegalArgumentException(inflationException)
        }
    }
}
