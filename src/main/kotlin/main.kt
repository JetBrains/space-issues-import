package com.jetbrains.space.import

import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.IssuesLoadResult
import com.jetbrains.space.import.common.IssuesLoader
import com.jetbrains.space.import.jira.JiraIssuesLoaderFactory
import com.jetbrains.space.import.notion.NotionIssuesLoaderFactory
import com.jetbrains.space.import.space.SpaceUploader
import com.jetbrains.space.import.youtrack.YoutrackIssuesLoaderFactory
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.types.ExternalIssue


@InternalAPI
fun main(args: Array<String>) = mainBody {
    CommandLineArgs(ArgParser(args)).run {
        val logger = LoggerFactory.getLogger(this.javaClass)

        if (debug) {
            logger.info("Running in debug mode")
        }

        fun List<Pair<String, String>>.mappingTransform() = toMap().mapKeys { it.key.lowercase() }

        val assigneeMapping = assigneeMapping.mappingTransform()
        val statusMapping = statusMapping.mappingTransform()
        val tagMapping = tagMapping.mappingTransform()

        runBlocking {
            val (loader, params) = getLoaderAndParams()
            val issuesLoadResult = loader.load(params)

            if (issuesLoadResult is IssuesLoadResult.Success) {
                // Preprocess issues: replace assignees and statuses according to the arguments
                val preprocessedIssues = issuesLoadResult.issues
                    .map {
                        val assignee = assigneeMapping[it.assignee?.lowercase()] ?: it.assignee
                        val status = statusMapping[it.status.lowercase()] ?: it.status
                        it.copy(assignee = assignee, status = status)
                    }
                val preprocessedTags = issuesLoadResult.tags.mapNotNull { (externalId, tags) ->
                    (externalId to tags.mapNotNull(tagMapping::get).toSet())
                        .takeIf { (_, tags) -> tags.isNotEmpty() }
                }.toMap()

                SpaceUploader()
                    .upload(
                        server = spaceServer,
                        token = spaceToken,

                        issues = preprocessedIssues,
                        projectIdentifier = spaceProject,
                        importSource = importSource,

                        assigneeMissingPolicy = assigneeMissingPolicy,
                        statusMissingPolicy = statusMissingPolicy,
                        onExistsPolicy = onExistsPolicy,
                        dryRun = dryRun,

                        batchSize = batchSize,

                        debug = debug,
                        boardIdentifier = spaceBoard,
                        tags = preprocessedTags,
                        tagPropertyMappingType = tagPropertyMappingType,
                    )
                logger.info("Finished")
            } else {
                logger.error("Failed to load issues from external system")
            }
        }
    }
}

private fun CommandLineArgs.getLoaderAndParams(): Pair<IssuesLoader, IssuesLoader.Params> {
    val (loader, query) = when (importSource) {
        ImportSource.Jira -> {
            val jiraUrl = jiraServer
            requireNotNull(jiraUrl) {
                IllegalArgumentException("jiraServer must be specified")
            }
            JiraIssuesLoaderFactory.create(jiraUrl, jiraUser, jiraPassword) to IssuesLoader.Params.Jira(jiraQuery ?: "")
        }
        ImportSource.Notion -> {
            val notionDatabaseId = notionDatabaseId
            val notionToken = notionToken
            requireNotNull(notionDatabaseId) {
                IllegalArgumentException("notionDatabaseId must be specified")
            }
            requireNotNull(notionToken) {
                IllegalArgumentException("notionToken must be specified")
            }

            NotionIssuesLoaderFactory.create(notionToken) to IssuesLoader.Params.Notion(
                query = notionQuery.orEmpty(),
                databaseId = notionDatabaseId,
                assigneeProperty = notionAssigneeProperty,
                assigneePropertyMappingType = notionAssigneePropertyMappingType,
                statusProperty = notionStatusProperty,
                statusPropertyMappingType = notionStatusPropertyMappingType,
                tagProperty = notionTagProperty,
                tagPropertyMappingType = notionTagPropertyMappingType,
            )
        }
        ImportSource.YouTrack, ImportSource.External -> {
            val youtrackServer = youtrackServer
            requireNotNull(youtrackServer) {
                IllegalArgumentException("youtrackServer must be specified")
            }

            YoutrackIssuesLoaderFactory.create(youtrackServer, youtrackToken) to IssuesLoader.Params.YouTrack(youtrackQuery ?: "")
        }
    }

    return loader to query
}

private fun ExternalIssue.copy(status: String, assignee: String?): ExternalIssue {
    return ExternalIssue(
        summary,
        description,
        status,
        assignee,
        externalId,
        externalName,
        externalUrl
    )
}
