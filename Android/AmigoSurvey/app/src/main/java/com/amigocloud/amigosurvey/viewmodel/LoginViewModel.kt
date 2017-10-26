package com.amigocloud.amigosurvey.viewmodel

import android.databinding.ObservableField
import com.amigocloud.amigosurvey.repository.Repository
import javax.inject.Inject

class LoginViewModel @Inject constructor(private val repo: Repository) {

    val email = ObservableField<String>()
    val password = ObservableField<String>()

    fun login() = repo.login(email.get(), password.get())
}
