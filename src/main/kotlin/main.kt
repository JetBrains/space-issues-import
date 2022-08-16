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
                // Preprocess issues: replace assignees, statuses and tags according to the arguments
                issuesLoadResult.issues
                    .forEach { it.resolveMappings(assigneeMapping, statusMapping, tagMapping) }

                SpaceUploader()
                    .upload(
                        server = spaceServer,
                        token = spaceToken,

                        issues = issuesLoadResult.issues,
                        projectIdentifier = spaceProject,
                        importSource = importSource,

                        assigneeMissingPolicy = assigneeMissingPolicy,
                        statusMissingPolicy = statusMissingPolicy,
                        onExistsPolicy = onExistsPolicy,
                        dryRun = dryRun,

                        batchSize = batchSize,

                        debug = debug,
                        boardIdentifier = spaceBoard,
                        tagPropertyMappingType = tagPropertyMappingType,
                    )
                logger.info("Finished")
            } else {
                logger.error("Failed to load issues from external system")
            }
        }
    }
}

private fun CommandLineArgs.getLoaderAndParams()
    = when (importSource) {
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
