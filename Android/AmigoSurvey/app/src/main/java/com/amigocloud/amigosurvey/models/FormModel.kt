package com.amigocloud.amigosurvey.models

data class FormModel(
        var base_form: String = "",
        var create_block_form: String = "",
        var create_block_json: String = "",
        var edit_block_form: String = "",
        var edit_block_json: String = "",
        var table_block_form: String = "",
        var table_block_json: String = ""
)