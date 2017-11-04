package com.amigocloud.amigosurvey.models

//data class SchemaItem (
//        var type: String? = null,
//        var name: String? = null,
//        var nullable: Boolean? = null,
//        var min_length: Int? = null,
//        var max_length: Int? = null,
//        var geometry_type: String? = null,
//        var default: String? = null,
//        var auto_populate: Boolean? = null,
//        var visible: Boolean? = null,
//        var editable: Boolean? = null,
//        var alias: String? = null,
//        var related_to: Int? = null,
//        var is_join : Boolean? = null,
//)

data class SchemaModel(
        var schema: List<Any>? = null
)

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
        var related_tables: String = "",
        var related_tables_hash: String = ""
)

data class Datasets(
        var count: Int = 0,
        var next: String? = null,
        var previous: String? = null,
        var results: List<DatasetModel> = listOf()
)