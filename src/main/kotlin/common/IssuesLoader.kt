package com.jetbrains.space.import.common

import com.jetbrains.space.import.space.IssueTemplate

interface IssuesLoader {
    suspend fun load(params: Params): IssuesLoadResult

    sealed interface Params {
        val query: String

        class Notion(
            override val query: String,
            val databaseId: String,
            val assigneeProperty: ExternalProjectProperty?,
            val assigneePropertyMappingType: ProjectPropertyType = defaultProjectPropertyType,
            val statusProperty: ExternalProjectProperty?,
            val statusPropertyMappingType: ProjectPropertyType = defaultProjectPropertyType,
            val tagProperty: ExternalProjectProperty?,
            val tagPropertyMappingType: ProjectPropertyType = defaultProjectPropertyType,
        ) : Params

        class YouTrack(
            override val query: String,
        ) : Params

        class Jira(
            override val query: String,
        ) : Params
    }
}

sealed interface IssuesLoadResult {
    data class Failed(val message: String) : IssuesLoadResult
    data class Success(val issues: List<IssueTemplate>) : IssuesLoadResult
}