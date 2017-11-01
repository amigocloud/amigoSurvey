package com.amigocloud.amigosurvey

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.amigocloud.amigosurvey.login.LoginViewModel
import com.amigocloud.amigosurvey.login.LoginViewState
import com.amigocloud.amigosurvey.models.UserModel
import com.amigocloud.amigosurvey.repository.AmigoRest
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.junit.MockitoRule

@RunWith(MockitoJUnitRunner::class)
class LoginViewModelTest {

    @Rule
    @JvmField
    val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @Rule
    @JvmField
    val executorRule = InstantTaskExecutorRule()

    @Mock lateinit var rest: AmigoRest
    @Mock lateinit var observer: Observer<LoginViewState>

    private lateinit var viewModel: LoginViewModel
    private val user = UserModel(email = "test@test.com")
    private val password = "password"

    @Before
    fun setup() {
        viewModel = LoginViewModel(rest)
        viewModel.events.observeForever(observer)
        viewModel.email.set(user.email)
        viewModel.password.set(password)
    }

    @Test
    fun loginSuccess() {
        `when`(rest.login(user.email, password)).thenReturn(Single.just(user))
        viewModel.onLogin()
        verify(observer, times(1)).onChanged(LoginViewState(user))
    }

    @Test
    fun loginError() {
        val error = Exception("Something failed!")
        `when`(rest.login(user.email, password)).thenReturn(Single.error(error))
        viewModel.onLogin()
        verify(observer, times(1)).onChanged(LoginViewState(error = error))
    }
}
