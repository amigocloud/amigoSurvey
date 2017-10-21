package com.amigocloud.amigosurvey.repository

import android.content.Context
import com.amigocloud.amigosurvey.models.AmigoToken
import com.amigocloud.amigosurvey.models.ProjectModel
import com.amigocloud.amigosurvey.models.Projects
import com.amigocloud.amigosurvey.models.UserModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by victor on 10/20/17.
 */

var disposable: Disposable? = null

class Repository {
    private object Holder {val instance = Repository()}

    companion object shared {
        val instance: Repository by lazy { Holder.instance }
    }

    var user: UserModel? = null

    fun login(email: String, password: String, context: Context) {
        val amigoRest = AmigoRest()
        disposable = amigoRest.login(email = email, password = password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->
                            val obj = result as AmigoToken
                            obj.save(context)
//                            fetchUser(context = context)
                        },
                        { error ->
                            print(error.message)
                        }
                )
    }

//    fun fetchUser(context: Context) : Observable<Projects> {
//        val amigoRest = AmigoRest()
//        return amigoRest.fetchUser(context = context)
//                .flatMap<UserModel> {
//                    { result -> Observable<Projects>
//                        this.user = result as UserModel
//                        amigoRest.fetchProjects(context = context)
//                    }
//                }
//    }

}