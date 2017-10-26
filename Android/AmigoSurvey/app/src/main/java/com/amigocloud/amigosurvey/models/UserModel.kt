package com.amigocloud.amigosurvey.models

/**
 * Created by victor on 10/20/17.
 */

data class UserModel(
        var email: String = "",
        var id: Long = 0,
        var custom_id: String = "",
        var first_name: String = "",
        var last_name: String = "",
        var organization: String = "",
        var visible_projects: String = "",
//    var visible_projects: [ProjectModel] = []
        var projects: Projects? = null
)