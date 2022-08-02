package com.jetbrains.space.import.space

import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.ProjectPropertyType
import com.xenomachina.argparser.SystemExitException
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
        issues: List<ExternalIssue>,
        projectIdentifier: ProjectIdentifier,
        importSource: ImportSource,
        assigneeMissingPolicy: ImportMissingPolicy,
        statusMissingPolicy: ImportMissingPolicy,
        onExistsPolicy: ImportExistsPolicy,
        dryRun: Boolean,
        batchSize: Int,
        debug: Boolean,
        boardIdentifier: SpaceBoardCustomIdentifier?,
        tags: Map<String, Set<String>>,
        tagPropertyMappingType: ProjectPropertyType,
    ): List<IssueImportResult> {
        val httpClient = createHttpClient()
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

        val spaceClient = SpaceHttpClient(httpClient).withPermanentToken(serverUrl = server, token = token)

        if (dryRun) logger.info("[DRY RUN]")

        val result = issues.chunked(batchSize).map { issuesBatched ->
            spaceClient.projects.planning.issues.importIssues(
                project = projectIdentifier,
                metadata = ImportMetadata(importSource.name),
                issues = issuesBatched,
                assigneeMissingPolicy = assigneeMissingPolicy,
                statusMissingPolicy = statusMissingPolicy,
                onExistsPolicy = onExistsPolicy,
                dryRun = dryRun
            ).also { response -> logger.info(response.message) }
        }

        if (!dryRun) {
            boardIdentifier?.let { spaceClient.addToBoard(projectIdentifier, boardIdentifier, result) }
            spaceClient.addTags(projectIdentifier, tags, tagPropertyMappingType, result)
        }

        return result
    }

    private suspend fun SpaceHttpClientWithCallContext.addToBoard(
        projectIdentifier: ProjectIdentifier,
        boardIdentifier: SpaceBoardCustomIdentifier,
        results: List<IssueImportResult>,
    ) {
        val boardId = when (boardIdentifier) {
            is SpaceBoardCustomIdentifier.Id -> boardIdentifier.id
            is SpaceBoardCustomIdentifier.Name -> getAllBoards(projectIdentifier).find { it.name == boardIdentifier.name }?.id
        } ?: return logger.error("no board with the name provided found, skipping")

        val issues = results.map { result ->
            ((result.created ?: emptyList()) + (result.updated ?: emptyList()))
                .mapNotNull(IssueImportResultItem::issue)
        }.flatten()

        issues.forEach { issue ->
            projects.planning.boards.issues.addIssueToBoard(IssueIdentifier.Id(issue.id), BoardIdentifier.Id(boardId))
        }
    }

    private suspend fun SpaceHttpClientWithCallContext.addTags(
        projectIdentifier: ProjectIdentifier,
        tags: Map<String, Set<String>>,
        tagPropertyMappingType: ProjectPropertyType,
        results: List<IssueImportResult>,
    ) {
        val preprocessedTags: Map<String, Set<String>> = when (tagPropertyMappingType) {
            ProjectPropertyType.Id -> tags
            ProjectPropertyType.Name -> {
                val allHierarchicalTagsMap = getAllHierarchicalTags(projectIdentifier)
                    .associate { tag -> tag.name to tag.id }

                tags.mapValues { (_, tags) ->
                    tags.mapNotNullTo(HashSet()) { tag -> allHierarchicalTagsMap[tag] }
                }
            }
            ProjectPropertyType.Email -> throw SystemExitException("can't map tags' emails", 2)
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

    private suspend fun SpaceHttpClientWithCallContext.getAllBoards(projectIdentifier: ProjectIdentifier): List<BoardRecord> {
        var lastResponse = projects.planning.boards.getAllBoards(projectIdentifier)
        val result: MutableList<BoardRecord> = mutableListOf()

        while (lastResponse.data.isNotEmpty()) {
            result += lastResponse.data
            lastResponse = projects.planning.boards.getAllBoards(
                project = projectIdentifier,
                batchInfo = BatchInfo(lastResponse.next, 100)
            )
        }

        return result
    }

    private suspend fun SpaceHttpClientWithCallContext.getAllHierarchicalTags(projectIdentifier: ProjectIdentifier): List<PlanningTag> {
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

    private fun createHttpClient(): HttpClient {
        return HttpClient(Apache) {
            engine {
                followRedirects = true
                socketTimeout = 60_000
                connectTimeout = 60_000
                connectionRequestTimeout = 60_000
            }
        }
    }
}
