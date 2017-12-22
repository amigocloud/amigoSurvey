package com.amigocloud.amigosurvey.login

import android.arch.lifecycle.Observer
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.R
import com.amigocloud.amigosurvey.databinding.ActivityLoginBinding
import com.amigocloud.amigosurvey.selector.SelectorActivity
import toothpick.Toothpick
import javax.inject.Inject
import android.content.Context.CONNECTIVITY_SERVICE



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

        viewModel.events.observe(this, Observer {
            it?.let { event ->
                event.error?.let {
                    Toast.makeText(this, "Login Failed", Toast.LENGTH_LONG).show()
                } ?: startActivity(Intent(this, SelectorActivity::class.java))
            }
        })

        if(!viewModel.isConnected()) {
            startActivity(Intent(this, SelectorActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Toothpick.closeScope(this)
    }

}
