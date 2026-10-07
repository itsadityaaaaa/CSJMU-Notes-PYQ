package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Converters
import com.example.data.model.ResourceEntity
import com.example.data.model.SubmissionEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS resources")
        db.execSQL("DROP TABLE IF EXISTS submissions")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS resources (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                collegeId TEXT NOT NULL,
                courseId TEXT NOT NULL,
                academicYear INTEGER NOT NULL,
                semester INTEGER NOT NULL,
                subjectId TEXT NOT NULL,
                subjectName TEXT NOT NULL,
                resourceType TEXT NOT NULL,
                examYear TEXT,
                fileUri TEXT,
                driveFileId TEXT,
                driveFileUrl TEXT,
                fileName TEXT,
                fileSizeFormatted TEXT,
                fileMimeType TEXT,
                contributorName TEXT NOT NULL,
                contributorEmail TEXT,
                isApproved INTEGER NOT NULL,
                publishedTimestamp INTEGER NOT NULL,
                downloadCount INTEGER NOT NULL,
                tags TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS submissions (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                collegeId TEXT NOT NULL,
                courseId TEXT NOT NULL,
                courseName TEXT NOT NULL,
                academicYear INTEGER NOT NULL,
                semester INTEGER NOT NULL,
                subjectId TEXT NOT NULL,
                subjectName TEXT NOT NULL,
                resourceType TEXT NOT NULL,
                examYear TEXT,
                fileUri TEXT,
                driveFileId TEXT,
                driveFileUrl TEXT,
                fileName TEXT,
                fileSizeFormatted TEXT,
                fileMimeType TEXT,
                fileSizeBytes INTEGER NOT NULL,
                contributorName TEXT NOT NULL,
                contributorEmail TEXT,
                status TEXT NOT NULL,
                adminFeedback TEXT,
                submittedTimestamp INTEGER NOT NULL,
                reviewedTimestamp INTEGER,
                expiresAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [ResourceEntity::class, SubmissionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resourceDao(): ResourceDao
    abstract fun submissionDao(): SubmissionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "csjmu_notes_pyq.db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
