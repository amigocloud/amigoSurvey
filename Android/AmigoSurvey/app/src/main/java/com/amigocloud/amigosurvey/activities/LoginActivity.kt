package com.amigocloud.amigosurvey.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.R
import com.amigocloud.amigosurvey.repository.Repository
import com.amigocloud.amigosurvey.viewmodel.LoginViewModel

import kotlinx.android.synthetic.main.activity_login.*
import toothpick.Toothpick
import javax.inject.Inject

class LoginActivity : AppCompatActivity() {

    @Inject lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }

        setContentView(R.layout.activity_login)

        email_sign_in_button.setOnClickListener { }

        repository.login(email = "victor@amigocloud.com", password = "vchernet757")

//        val amigoRest = AmigoRest()
//        disposable = amigoRest.login(email = "victor@amigocloud.com", password = "vchernet757")
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(
//                                { result ->
//                                    val obj = result as AmigoToken
//                                    obj.save(this.context)
//                                    fetchUser()
//                                },
//                                { error ->
//                                    print(error.message)
//                                }
//                        )

    }

    override fun onDestroy() {
        super.onDestroy()
        Toothpick.closeScope(this)
    }

//    fun fetchUser() {
//        val amigoRest = AmigoRest()
//        disposable = amigoRest.fetchUser(this)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        { result ->
//                            val obj = result as UserModel
//                            print(obj.toString())
//                        },
//                        { error ->
//                            print(error.message)
//                        }
//                )
//    }

}
