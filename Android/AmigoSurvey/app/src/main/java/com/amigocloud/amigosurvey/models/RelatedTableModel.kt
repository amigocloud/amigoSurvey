package com.amigocloud.amigosurvey.models

/**
 * Created by victor on 10/21/17.
 */

data class RelatedTableModel(
        var id: Long = 0,
        var name: String = "",
        var chunked_upload: String = "",
        var chunked_upload_complete: String = "",
        var schema: String = "",
        var table_name: String = "",
        var type: String = ""
)

data class RelatedTables(
        var count: Int? = null,
        var next: String? = null,
        var previous: String? = null,
        var results: List<RelatedTableModel>? = null
)