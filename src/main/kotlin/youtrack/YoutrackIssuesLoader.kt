package com.jetbrains.space.import.youtrack

import com.jetbrains.space.import.common.IssuesLoadResult
import com.jetbrains.space.import.common.IssuesLoader
import org.jetbrains.youtrack.rest.YouTrack
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.types.ExternalIssue


object YoutrackIssuesLoaderFactory {
    fun create(serverUrl: String, token: String?): IssuesLoader {
        return YoutrackIssuesLoader(serverUrl, token)
    }
}

class YoutrackIssuesLoader(private val serverUrl: String, private val token: String?) : IssuesLoader {
    companion object {
        private val logger = LoggerFactory.getLogger(YoutrackIssuesLoader::class.java)
    }

    override suspend fun load(query: String) : IssuesLoadResult {
        return try {
            val youtrack = with(YouTrack) {
                if (token.isNullOrBlank()) authorizeAsGuest(serverUrl) else authorizeByPermanentToken(serverUrl, token)
            }

            val issuesCount = youtrack.issues(query).count()
            val issues = youtrack.issues(query).mapIndexed { issueIndex, it ->
                val issue = ExternalIssue(
                    summary = it.summary,
                    description = it.description,
                    assignee = it["Assignee"] ?: "",
                    status = it.state.name,
                    externalId = it.id,
                    externalName = null,
                    externalUrl = "$serverUrl/issue/${it.id}"
                )

                logger.info("issue ${issueIndex + 1} / $issuesCount")
                logger.info("id         \t${it.id}")
                logger.info("status     \t${issue.status}")
                logger.info("assignee   \t${issue.assignee}")
                logger.info("summary    \t${issue.summary}")
                logger.info("description\t${issue.description?.take(64)?.lines()?.joinToString(" ")}...")
                logger.info("---")

                issue
            }

            IssuesLoadResult.Success(issues.toList())
        } catch (e: Exception) {
            logger.error(e.toString())
            IssuesLoadResult.Failed(e.message ?: "unknown exception")
        }
    }
}
