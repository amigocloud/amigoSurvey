package com.amigocloud.amigosurvey.login

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.net.ConnectivityManager
import android.text.TextUtils
import android.util.Log
import com.amigocloud.amigosurvey.models.UserModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import com.amigocloud.amigosurvey.viewmodel.INFLATION_EXCEPTION
import com.amigocloud.amigosurvey.viewmodel.ViewModelFactory
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

data class LoginViewState(val user: UserModel? = null, val error: Throwable? = null)

class LoginViewModel(private val rest: AmigoRest, private val connectivityManager: ConnectivityManager) : ViewModel() {
    val TAG = "LoginViewModel"

    private val loginProcessor = PublishProcessor.create<Pair<String, String>>()

    val email = ObservableField<String>()
    val password = ObservableField<String>()
    val isLoading = ObservableBoolean(false)

    val events: LiveData<LoginViewState> = LiveDataReactiveStreams.fromPublisher(loginProcessor
            .flatMapSingle { (email, password) ->
                rest.login(email, password)
                        .map { LoginViewState(user = it) }
                        .onErrorReturn { LoginViewState(error = it) }
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

    fun isConnected(): Boolean {
        try {
            val activeNetwork = connectivityManager.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val rest: AmigoRest, private val connectivityManager: ConnectivityManager) : ViewModelFactory<LoginViewModel>() {

        override val modelClass = LoginViewModel::class.java

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(this.modelClass)) {
                return LoginViewModel(rest, connectivityManager) as T
            }
            throw IllegalArgumentException(INFLATION_EXCEPTION)
        }
    }
}
