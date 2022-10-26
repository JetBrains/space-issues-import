package com.jetbrains.space.import

import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.IssuesLoadResult
import com.jetbrains.space.import.common.IssuesLoader
import com.jetbrains.space.import.github.GitHubIssuesLoaderFactory
import com.jetbrains.space.import.jira.JiraIssuesLoaderFactory
import com.jetbrains.space.import.notion.NotionIssuesLoaderFactory
import com.jetbrains.space.import.space.SpaceUploader
import com.jetbrains.space.import.space.SpaceUploaderImpl
import com.jetbrains.space.import.youtrack.YoutrackIssuesLoaderFactory
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


@InternalAPI
fun main(args: Array<String>) = mainBody(columns = 140) {
    val spaceUploader: SpaceUploader = SpaceUploaderImpl()
    val getLoaderAndParams: (() -> Pair<IssuesLoader, IssuesLoader.Params>)? = null

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
            val (loader, params) = getLoaderAndParams?.invoke() ?: getLoaderAndParams()
            val issuesLoadResult = loader.load(params)

            if (issuesLoadResult is IssuesLoadResult.Success) {
                // Preprocess issues: replace assignees, statuses and tags according to the arguments
                issuesLoadResult.issues
                    .forEach { it.resolveMappings(assigneeMapping, statusMapping, tagMapping) }

                spaceUploader
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
            val jiraServer = jiraServer
            requiredArgument("jiraServer", jiraServer)
            JiraIssuesLoaderFactory.create(jiraServer, jiraUser, jiraPassword) to IssuesLoader.Params.Jira(jiraQuery)
        }
        ImportSource.Notion -> {
            val notionDatabaseId = notionDatabaseId
            val notionToken = notionToken
            requiredArgument("notionDatabaseId", notionDatabaseId)
            requiredArgument("notionToken", notionToken)

            NotionIssuesLoaderFactory.create(notionToken) to IssuesLoader.Params.Notion(
                query = notionQuery,
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
            requiredArgument("youtrackServer", youtrackServer)

            YoutrackIssuesLoaderFactory.create(youtrackServer, youtrackToken) to IssuesLoader.Params.YouTrack(youtrackQuery)
        }
        ImportSource.GitHub -> {
            val gitHubRepositoryOwner = gitHubRepositoryOwner
            val gitHubRepository = gitHubRepository
            requiredArgument("gitHubRepositoryOwner", gitHubRepositoryOwner)
            requiredArgument("gitHubRepository", gitHubRepository)

            GitHubIssuesLoaderFactory.create(gitHubAuthorization) to IssuesLoader.Params.GitHub(
                owner = gitHubRepositoryOwner,
                repository = gitHubRepository
            )
        }
    }

@OptIn(ExperimentalContracts::class)
fun <T> requiredArgument(name: String, value: T?) {
    contract {
        returns() implies (value != null)
    }
    requireNotNull(value) { "$name must be specified" }
}
