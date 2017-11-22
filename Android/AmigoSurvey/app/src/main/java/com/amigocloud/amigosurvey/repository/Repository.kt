package com.amigocloud.amigosurvey.repository

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.amigocloud.amigosurvey.models.RelatedRecord
import com.amigocloud.amigosurvey.models.RelatedRecordDao
import javax.inject.Inject
import javax.inject.Singleton

@Database(entities = arrayOf(RelatedRecord::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun relatedRecordDao(): RelatedRecordDao
}

@Singleton
class Repository @Inject constructor(val db: AppDatabase) {
    fun relatedRecordDao(): RelatedRecordDao = db.relatedRecordDao()
}