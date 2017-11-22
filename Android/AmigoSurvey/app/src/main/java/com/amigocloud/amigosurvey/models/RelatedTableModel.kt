package com.amigocloud.amigosurvey.models

import android.arch.persistence.room.*

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

@Entity(tableName = "RelatedRecord")
data class RelatedRecord(
        @ColumnInfo(name = "filename")
        var filename: String = "",

        @ColumnInfo(name = "source_amigo_id")
        var source_amigo_id: String = "",

        @ColumnInfo(name = "datetime")
        var datetime: String = "",

        @ColumnInfo(name = "location")
        var location: String = "",

        @PrimaryKey
        @ColumnInfo(name = "amigo_id")
        var amigo_id: String = "",

        @ColumnInfo(name = "relatedTableId")
        var relatedTableId: String = "",

        @ColumnInfo(name = "recordsTotal")
        var recordsTotal: Int = 0
)

@Dao
interface RelatedRecordDao {
    @get:Query("SELECT * FROM RelatedRecord")
    val all: List<RelatedRecord>

    @Query("SELECT * FROM RelatedRecord WHERE source_amigo_id = :source_amigo_id")
    fun findRecords(source_amigo_id: String): List<RelatedRecord>

    @Insert
    fun insert(record: RelatedRecord)

    @Delete
    fun delete(record: RelatedRecord)
}
