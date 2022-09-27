package com.jetbrains.space.import.common

import com.jetbrains.space.import.space.IssueTemplate
import kotlin.reflect.KClass

interface IssuesLoader {
    suspend fun load(params: Params): IssuesLoadResult

    sealed interface Params {

        class Notion(
            val query: String?,
            val databaseId: String,
            val assigneeProperty: ExternalProjectProperty?,
            val assigneePropertyMappingType: ProjectPropertyType = defaultProjectPropertyType,
            val statusProperty: ExternalProjectProperty?,
            val statusPropertyMappingType: ProjectPropertyType = defaultProjectPropertyType,
            val tagProperty: ExternalProjectProperty?,
            val tagPropertyMappingType: ProjectPropertyType = defaultProjectPropertyType,
        ) : Params

        class YouTrack(
            val query: String?
        ) : Params

        class Jira(
            val query: String?
        ) : Params

        class GitHub(
            val owner: String,
            val repository: String
        ) : Params
    }
}

sealed interface IssuesLoadResult {
    data class Failed(val message: String) : IssuesLoadResult {
        companion object {
            fun messageOrUnknownException(e: Exception)
                = Failed(e.message ?: "unknown exception")

            fun <T : Any> wrongParams(loader: KClass<T>)
                = Failed("wrong parameters passed to ${loader.simpleName}")
        }
    }
    data class Success(val issues: List<IssueTemplate>) : IssuesLoadResult
}
