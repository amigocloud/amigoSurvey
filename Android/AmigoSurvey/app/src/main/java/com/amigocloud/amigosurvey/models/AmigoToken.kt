package com.amigocloud.amigosurvey.models

data class AmigoToken(
        var access_token: String = "",
        var token_type: String = "",
        var expires_in: Long = 0,
        var refresh_token: String = "",
        var scope: String = ""
)

