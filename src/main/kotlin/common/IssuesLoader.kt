package com.jetbrains.space.import.common

import space.jetbrains.api.runtime.types.ExternalIssue

interface IssuesLoader {
    suspend fun load(query: String): IssuesLoadResult
}

sealed class IssuesLoadResult {
    data class Failed(val message: String) : IssuesLoadResult()
    data class Success(val issues: List<ExternalIssue>) : IssuesLoadResult()
}