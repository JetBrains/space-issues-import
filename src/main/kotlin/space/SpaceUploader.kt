package com.jetbrains.space.import.space

import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.ProjectPropertyType
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.logging.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.*
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.*

class SpaceUploader {
    companion object {
        private val logger = LoggerFactory.getLogger(SpaceUploader::class.java)
    }

    @InternalAPI
    suspend fun upload(
        server: String,
        token: String,
        issues: List<IssueTemplate>,
        projectIdentifier: ProjectIdentifier,
        importSource: ImportSource,
        assigneeMissingPolicy: ImportMissingPolicy,
        statusMissingPolicy: ImportMissingPolicy,
        onExistsPolicy: ImportExistsPolicy,
        dryRun: Boolean,
        batchSize: Int,
        debug: Boolean,
        tagPropertyMappingType: ProjectPropertyType? = null,
    ): List<IssueImportResult> {
        val httpClient = ktorClientForSpace()
            .let {
                if (debug) {
                    it.config {
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                    }
                } else {
                    it
                }
            }

        val spaceClient = SpaceClient(httpClient, serverUrl = server, token = token)

        if (dryRun) logger.info("[DRY RUN]")

        val result = issues.chunked(batchSize).map { issuesBatched ->
            spaceClient.projects.planning.issues.importIssues(
                project = projectIdentifier,
                metadata = ImportMetadata(importSource.name),
                issues = issuesBatched.map { it.externalIssue },
                assigneeMissingPolicy = assigneeMissingPolicy,
                statusMissingPolicy = statusMissingPolicy,
                onExistsPolicy = onExistsPolicy,
                dryRun = dryRun
            ).also { response -> logger.info(response.message) }
        }

        if (!dryRun) {
            tagPropertyMappingType?.let { spaceClient.addTags(projectIdentifier, issues, tagPropertyMappingType, result) }
        }

        return result
    }

    private suspend fun SpaceClient.addTags(
        projectIdentifier: ProjectIdentifier,
        issues: List<IssueTemplate>,
        tagPropertyMappingType: ProjectPropertyType,
        results: List<IssueImportResult>,
    ) {
        val preprocessedTags: Map<String, Set<String>> = when (tagPropertyMappingType) {
            ProjectPropertyType.Id -> issues.associate { it.externalIssue.externalId to it.tags }
            ProjectPropertyType.Name -> {
                val allHierarchicalTagsMap = getAllHierarchicalTags(projectIdentifier)
                    .associate { tag -> tag.name to tag.id }

                issues.associate {
                    it.externalIssue.externalId to it.tags.mapNotNullTo(HashSet()) {
                            tag -> allHierarchicalTagsMap[tag]
                    }
                }
            }
            ProjectPropertyType.Email -> throw IllegalArgumentException("can't map tags' emails")
        }

        for (result in results) {
            val items = ((result.created ?: emptyList()) + (result.updated ?: emptyList()))
                .filterNot { it.issue == null }

            for (item in items) {
                val tagIds = item.externalId?.let(preprocessedTags::get)?.takeIf(Set<String>::isNotEmpty)
                    ?: continue

                for (tagId in tagIds) {
                    projects.planning.issues.tags.addIssueTag(
                        project = projectIdentifier,
                        issueId = IssueIdentifier.Id(item.issue!!.id),
                        tagId = tagId,
                    )
                }
            }
        }
    }

    private suspend fun SpaceClient.getAllHierarchicalTags(projectIdentifier: ProjectIdentifier): List<PlanningTag> {
        var lastResponse = projects.planning.tags.getAllHierarchicalTags(projectIdentifier)
        val result: MutableList<PlanningTag> = mutableListOf()

        while (lastResponse.data.isNotEmpty()) {
            result += lastResponse.data
            lastResponse = projects.planning.tags.getAllHierarchicalTags(
                project = projectIdentifier,
                batchInfo = BatchInfo(lastResponse.next, 100)
            )
        }

        return result
    }
}
