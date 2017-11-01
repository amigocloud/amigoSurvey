package com.amigocloud.amigosurvey.login

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.text.TextUtils
import com.amigocloud.amigosurvey.models.UserModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

data class LoginEvent(val user: UserModel?, val error: Throwable?)

class LoginViewModel(private val rest: AmigoRest) : ViewModel() {

    private val loginProcessor = PublishProcessor.create<Pair<String, String>>()

    val email = ObservableField<String>()
    val password = ObservableField<String>()
    val isLoading = ObservableBoolean(false)

    val events: LiveData<LoginEvent> = LiveDataReactiveStreams.fromPublisher(loginProcessor
            .flatMapSingle { (email, password) ->
                rest.login(email, password)
                        .map { LoginEvent(it, null) }
                        .onErrorReturn { LoginEvent(null, it) }
            }
            .doAfterNext { isLoading.set(false) })

    fun onLogin() {
        val email = email.get()
        val password = password.get()
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            isLoading.set(true)
            loginProcessor.onNext(email.to(password))
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest) : ViewModelFactory<LoginViewModel>() {

        override val modelClass = LoginViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return LoginViewModel(rest) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }
}
