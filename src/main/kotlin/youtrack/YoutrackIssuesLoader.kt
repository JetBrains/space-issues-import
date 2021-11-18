package com.jetbrains.space.import.youtrack

import com.jetbrains.space.import.common.IssuesLoadResult
import com.jetbrains.space.import.common.IssuesLoader
import org.jetbrains.youtrack.rest.YouTrack
import org.jetbrains.youtrack.rest.YouTrackClientException
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.types.ExternalIssue
import java.net.URL


object YoutrackIssuesLoaderFactory {
    fun create(serverUrl: String, token: String?): IssuesLoader {
        return YoutrackIssuesLoader(serverUrl, token)
    }
}

class YoutrackIssuesLoader(private val serverUrl: String, private val token: String?) : IssuesLoader {
    companion object {
        private val logger = LoggerFactory.getLogger(YoutrackIssuesLoader::class.java)
    }

    override suspend fun load(params: IssuesLoader.Params) : IssuesLoadResult {
        return try {
            val url = URL(serverUrl).toString().trimEnd('/')
            val youtrack = with(YouTrack) {
                if (token.isNullOrBlank()) authorizeAsGuest(url) else authorizeByPermanentToken(url, token)
            }

            val issuesCount = youtrack.issues(params.query).count()
            val issues = youtrack.issues(params.query).mapIndexedNotNull { issueIndex, it ->
                try {
                    val issue = ExternalIssue(
                        summary = it.summary,
                        description = it.description,
                        assignee = it["Assignee"] ?: "",
                        status = it.state.name,
                        externalId = it.id,
                        externalName = null,
                        externalUrl = "$url/issue/${it.id}"
                    )

                    logger.info("issue ${issueIndex + 1} / $issuesCount")
                    logger.info("id         \t${it.id}")
                    logger.info("status     \t${issue.status}")
                    logger.info("assignee   \t${issue.assignee}")
                    logger.info("summary    \t${issue.summary}")
                    logger.info("description\t${issue.description?.take(64)?.lines()?.joinToString(" ")}...")
                    logger.info("---")

                    issue
                } catch (e: NullPointerException) {
                    logger.error("failed to parse issue ${issueIndex + 1} / $issuesCount from YouTrack")
                    null
                }
            }.toList()

            if (issues.count() != issuesCount) {
                logger.error("some issues failed to parse, report the problem here: https://github.com/JetBrains/space-issues-import/issues/new")
            }

            IssuesLoadResult.Success(issues, emptyMap())
        } catch (e: YouTrackClientException) {
            logger.error("failed to parse YT response. Does `--youtrackServer` URL match the one in your YT Domain Settings? Typically, it should end with /youtrack")
            logger.error("original error message: $e")
            logger.error("if the problem still persists, report it here: https://github.com/JetBrains/space-issues-import/issues/new")

            IssuesLoadResult.Failed(e.message ?: "unknown exception")
        } catch (e: Exception) {
            logger.error(e.toString())
            logger.error("report the problem here: https://github.com/JetBrains/space-issues-import/issues/new")

            IssuesLoadResult.Failed(e.message ?: "unknown exception")
        }
    }
}
