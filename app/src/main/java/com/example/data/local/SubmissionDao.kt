package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SubmissionEntity
import com.example.domain.model.ApprovalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionDao {
    @Query("SELECT * FROM submissions ORDER BY submittedTimestamp DESC")
    fun getAllSubmissions(): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE status = 'PENDING' ORDER BY submittedTimestamp ASC")
    fun getPendingSubmissions(): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE status = :status ORDER BY submittedTimestamp DESC")
    fun getSubmissionsByStatus(status: ApprovalStatus): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE contributorName = :uploaderName ORDER BY submittedTimestamp DESC")
    fun getSubmissionsByUploader(uploaderName: String): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE id = :id LIMIT 1")
    fun getSubmissionById(id: String): Flow<SubmissionEntity?>

    @Query("SELECT * FROM submissions WHERE id = :id LIMIT 1")
    suspend fun findSubmissionById(id: String): SubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity)

    @Update
    suspend fun updateSubmission(submission: SubmissionEntity)

    @Query("DELETE FROM submissions WHERE id = :id")
    suspend fun deleteSubmission(id: String)
}
