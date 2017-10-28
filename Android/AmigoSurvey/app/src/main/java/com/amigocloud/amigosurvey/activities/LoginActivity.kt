package com.amigocloud.amigosurvey.activities

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.R
import com.amigocloud.amigosurvey.databinding.ActivityLoginBinding
import com.amigocloud.amigosurvey.viewmodel.LoginViewModel
import io.reactivex.android.schedulers.AndroidSchedulers

import toothpick.Toothpick
import javax.inject.Inject

class LoginActivity : AppCompatActivity() {

    @Inject lateinit var viewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.viewModel = viewModel
        binding.emailSignInButton.setOnClickListener {
            viewModel.login()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe{ token ->

                    }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Toothpick.closeScope(this)
    }

}
