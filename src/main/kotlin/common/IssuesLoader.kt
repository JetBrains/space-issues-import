package com.jetbrains.space.import.common

import space.jetbrains.api.runtime.types.ExternalIssue

interface IssuesLoader {
    suspend fun load(params: Params): IssuesLoadResult

    sealed interface Params {
        val query: String

        class Notion(
            override val query: String,
            val databaseId: String,
            val assigneeProperty: ExternalProjectProperty?,
            val assigneePropertyMappingType: ProjectPropertyType,
            val statusProperty: ExternalProjectProperty?,
            val statusPropertyMappingType: ProjectPropertyType,
            val tagProperty: ExternalProjectProperty?,
            val tagPropertyMappingType: ProjectPropertyType,
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
    data class Success(val issues: List<ExternalIssue>, val tags: Map<String, Set<String>>) : IssuesLoadResult
}