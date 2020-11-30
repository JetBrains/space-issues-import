package com.jetbrains.space.import

import com.jetbrains.space.import.common.IssuesLoadResult
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

        val assigneeMapping = assigneeMapping.toMap()
        val statusMapping = statusMapping.toMap()

        runBlocking {
            val loader = YoutrackIssuesLoaderFactory.create(youtrackServer, youtrackToken)
            val result = loader.load(youtrackQuery)

            if (result is IssuesLoadResult.Success) {
                // Preprocess issues: replace assignees and statuses according to the arguments
                val preprocessedIssues = result.issues.map {
                    val assignee = assigneeMapping[it.assignee] ?: it.assignee
                    val status = statusMapping[it.status] ?: it.status
                    it.copy(assignee = assignee, status = status)
                }

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
                        dryRun = dryRun
                    )
            } else {
                logger.error("Failed to load issues from external system")
            }
        }
    }
}

private fun ExternalIssue.copy(status: String, assignee: String?): ExternalIssue {
    return ExternalIssue(summary, description, status, assignee, externalId, externalName, externalUrl)
}
