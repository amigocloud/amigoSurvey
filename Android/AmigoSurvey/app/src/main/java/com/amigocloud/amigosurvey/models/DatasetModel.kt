package com.amigocloud.amigosurvey.models

/**
 * Created by victor on 10/21/17.
 */

data class DatasetModel(
        var id: Long = 0,
        var name: String = "",
        var boundingbox: String = "",
        var tiles: String = "",
        var url: String = "",
        var visible: Boolean = false,
        var type: String = "",
        var preview_image: String = "",
        var preview_image_hash: String = "",
        var read_only: Boolean = false,
        var online_only: Boolean = false,
        var display_field: String = "",
        var auto_sync: Boolean = false,
        var table_name: String = "",
        var master_state: String = "",
        var schema: String = "",
        var submit_change: String = "",
        var forms_summary: String = "",

        var schema_hash: String = "",
//    var schema_fields: [String] = []

        var related_tables: String = "",
        var related_tables_hash: String = ""
//    var related_tables: [RelatedTableModel] = []

//    var formModel = FormModel()
)

data class Datasets(
        var count: Int = 0,
        var next: String? = null,
        var previous: String? = null,
        var results: List<DatasetModel> = listOf()
)