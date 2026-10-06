package com.example.data.repository

import com.example.data.local.ResourceDao
import com.example.data.model.ResourceEntity
import com.example.data.remote.IRemoteResourceDataSource
import com.example.domain.model.ResourceItem
import com.example.domain.model.ResourceType
import com.example.domain.repository.IResourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ResourceRepositoryImpl(
    private val resourceDao: ResourceDao,
    private val remoteDataSource: IRemoteResourceDataSource,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : IResourceRepository {

    override fun getResources(
        collegeId: String,
        courseId: String,
        academicYear: Int,
        semester: Int?,
        resourceType: ResourceType?
    ): Flow<List<ResourceItem>> {
        // Trigger background Firestore cloud sync
        scope.launch {
            val remoteResult = remoteDataSource.fetchRemoteResources(
                collegeId = collegeId,
                courseId = courseId,
                academicYear = academicYear,
                semester = semester
            )
            remoteResult.getOrNull()?.forEach { item ->
                resourceDao.insertOrUpdate(ResourceEntity.fromDomain(item))
            }
        }

        return resourceDao.getFilteredResources(
            collegeId = collegeId,
            courseId = courseId,
            academicYear = academicYear,
            semester = semester,
            resourceType = resourceType
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun searchResources(
        query: String,
        collegeId: String?,
        courseId: String?
    ): Flow<List<ResourceItem>> {
        return resourceDao.searchResources(query, collegeId, courseId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getResourceById(id: String): Flow<ResourceItem?> {
        return resourceDao.getResourceById(id).map { it?.toDomain() }
    }

    override suspend fun insertOrUpdateResource(resource: ResourceItem) {
        resourceDao.insertOrUpdate(ResourceEntity.fromDomain(resource))
        remoteDataSource.publishResourceToRemote(resource)
    }

    override suspend fun deleteResource(id: String) {
        resourceDao.deleteById(id)
    }
}
