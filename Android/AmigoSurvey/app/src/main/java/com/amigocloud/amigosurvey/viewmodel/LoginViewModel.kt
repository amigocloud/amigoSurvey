package com.amigocloud.amigosurvey.viewmodel

import android.databinding.ObservableField
import android.os.Bundle
import com.amigocloud.amigosurvey.repository.Repository
import javax.inject.Inject

class LoginViewModel @Inject constructor(private val repo: Repository) {

    val email = ObservableField<String>()
    val password = ObservableField<String>()

    fun login() =
            repo.login(email.get(), password.get())
                    .flatMap { repo.fetchUser() }
//
//    fun save(bundle: Bundle) {
//        bundle.putString("email", email.get())
//        bundle.putString("password", password.get())
//    }
//
//    fun load(bundle: Bundle) {
//        email.set(bundle.getString("email"))
//        password.set(bundle.getString("password"))
//    }
}
