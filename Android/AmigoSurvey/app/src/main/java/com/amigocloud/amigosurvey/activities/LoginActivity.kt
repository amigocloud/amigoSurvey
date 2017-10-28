package com.amigocloud.amigosurvey.activities

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.R
import com.amigocloud.amigosurvey.databinding.ActivityLoginBinding
import com.amigocloud.amigosurvey.viewmodel.LoginViewModel
import io.reactivex.android.schedulers.AndroidSchedulers

import toothpick.Toothpick
import javax.inject.Inject

class LoginActivity : AppCompatActivity() {

    @Inject lateinit var viewModelFactory: LoginViewModel.Factory

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }
        viewModel = viewModelFactory.get(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.viewModel = viewModel
        binding.emailSignInButton.setOnClickListener {
            viewModel.login()
                    .andThen(viewModel.fetchUser())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ user ->
                        Log.d("UserModel", user.toString())
                        Toast.makeText(this, "Login Success", Toast.LENGTH_LONG).show()
                    }, {
                        Toast.makeText(this, "Login Failed", Toast.LENGTH_LONG).show()
                    })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Toothpick.closeScope(this)
    }

}
