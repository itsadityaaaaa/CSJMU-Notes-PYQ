package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ResourceEntity
import com.example.domain.model.ResourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    @Query("""
        SELECT * FROM resources 
        WHERE collegeId = :collegeId 
          AND courseId = :courseId 
          AND academicYear = :academicYear
          AND (:semester IS NULL OR semester = :semester)
          AND (:resourceType IS NULL OR resourceType = :resourceType)
        ORDER BY publishedTimestamp DESC
    """)
    fun getFilteredResources(
        collegeId: String,
        courseId: String,
        academicYear: Int,
        semester: Int?,
        resourceType: ResourceType?
    ): Flow<List<ResourceEntity>>

    @Query("""
        SELECT * FROM resources 
        WHERE (title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
           OR subjectName LIKE '%' || :query || '%'
           OR fileName LIKE '%' || :query || '%'
           OR contributorName LIKE '%' || :query || '%')
          AND (:collegeId IS NULL OR collegeId = :collegeId)
          AND (:courseId IS NULL OR courseId = :courseId)
        ORDER BY publishedTimestamp DESC
    """)
    fun searchResources(
        query: String,
        collegeId: String?,
        courseId: String?
    ): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources WHERE id = :id LIMIT 1")
    fun getResourceById(id: String): Flow<ResourceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(resource: ResourceEntity)

    @Query("DELETE FROM resources WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM resources")
    suspend fun count(): Int
}
