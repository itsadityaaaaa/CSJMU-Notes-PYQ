package com.example.domain.repository

import com.example.domain.model.ResourceItem
import com.example.domain.model.ResourceType
import kotlinx.coroutines.flow.Flow

/**
 * Scalable domain repository contract for academic resources.
 * Supports local caching (Room) and remote synchronization (Firebase / Cloud APIs).
 */
interface IResourceRepository {
    fun getResources(
        collegeId: String,
        courseId: String,
        academicYear: Int,
        semester: Int? = null,
        resourceType: ResourceType? = null
    ): Flow<List<ResourceItem>>

    fun searchResources(
        query: String,
        collegeId: String? = null,
        courseId: String? = null
    ): Flow<List<ResourceItem>>

    fun getResourceById(id: String): Flow<ResourceItem?>

    suspend fun insertOrUpdateResource(resource: ResourceItem)

    suspend fun deleteResource(id: String)
}
