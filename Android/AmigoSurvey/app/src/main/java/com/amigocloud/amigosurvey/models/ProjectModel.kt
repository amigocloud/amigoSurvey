package com.amigocloud.amigosurvey.models

data class ProjectModel(

        var id: Long = 0,
        var name: String = "",
        var hash: String = "",
        var description: String = "",
        var organization: String = "",
        var permission_level: String = "",
        var url: String = "",
        var datasets: String = "",
        var submit_changeset: String = "",
        var preview_image: String = "",
        var preview_image_hash: String = "",
        var support_files: String = "",
        var support_files_hash: String = ""
)

data class Projects(
        var count: Int = 0,
        var next: String? = null,
        var previous: String? = null,
        var results: List<ProjectModel> = listOf()
)