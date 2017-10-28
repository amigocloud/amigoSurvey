package com.amigocloud.amigosurvey.repository

import com.amigocloud.amigosurvey.models.UserModel
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(private val amigoRest: AmigoRest) {
    var user: UserModel? = null

    fun login(email: String, password: String) = amigoRest.login(email, password)


    fun fetchUser() : Single<UserModel> {
        return amigoRest.fetchUser().zipWith(amigoRest.fetchProjects(), BiFunction {
            user, projects -> user
        })
    }
}