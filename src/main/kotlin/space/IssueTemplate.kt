package com.jetbrains.space.import.space

import space.jetbrains.api.runtime.types.ExternalIssue

private typealias Mapping = Map<String, String>

class IssueTemplate(
    externalIssue: ExternalIssue,
    tags: Set<String> = emptySet()
) {
    var externalIssue = externalIssue
        private set

    var tags = tags
        private set

    fun resolveMappings(assigneeMapping: Mapping, statusMapping: Mapping, tagMapping: Mapping) {
        val assignee = assigneeMapping[externalIssue.assignee?.lowercase()] ?: externalIssue.assignee
        val status = statusMapping[externalIssue.status.lowercase()] ?: externalIssue.status

        externalIssue = externalIssue.copy(assignee = assignee, status = status)
        tags = tags.mapTo(HashSet()) { tagMapping[it] ?: it }
    }
}

private fun ExternalIssue.copy(status: String, assignee: String?): ExternalIssue {
    return ExternalIssue(
        summary = summary,
        description = description,
        status = status,
        assignee = assignee,
        externalId = externalId,
        externalName = externalName,
        externalUrl = externalUrl
    )
}
