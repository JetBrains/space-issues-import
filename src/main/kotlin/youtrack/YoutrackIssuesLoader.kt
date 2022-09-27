package com.jetbrains.space.import.youtrack

import com.jetbrains.space.import.common.*
import com.jetbrains.space.import.space.IssueTemplate
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

private class YoutrackIssuesLoader(private val serverUrl: String, private val token: String?) : IssuesLoader {
    companion object {
        private val logger = LoggerFactory.getLogger(YoutrackIssuesLoader::class.java)
        const val defaultYouTrackQuery = "sort by: created asc"
    }

    override suspend fun load(params: IssuesLoader.Params) : IssuesLoadResult {
        if (params !is IssuesLoader.Params.YouTrack)
            return IssuesLoadResult.Failed.wrongParams(YoutrackIssuesLoader::class)

        val query = params.query ?: defaultYouTrackQuery

        return try {
            val url = URL(serverUrl).toString().trimEnd('/')
            val (issuesCount, issues) = with(YouTrack) {
                if (token.isNullOrBlank()) authorizeAsGuest(url) else authorizeByPermanentToken(url, token)
            }
                .use { youtrack ->
                    val issuesCount = youtrack.issues(query).count()
                    issuesCount to youtrack.issues(query).mapIndexedNotNull { issueIndex, it ->
                        try {
                            ExternalIssue(
                                summary = it.summary,
                                description = it.description,
                                assignee = it["Assignee"] ?: "",
                                status = it.state.name,
                                externalId = it.id,
                                externalName = null,
                                externalUrl = "$url/issue/${it.id}"
                            )
                                .also { logger.info(it, issueIndex, issuesCount) }
                        } catch (e: NullPointerException) {
                            logger.failedToParseIssue(issueIndex, issuesCount, "YouTrack")
                            null
                        }
                    }.toList()
                }

            if (issues.count() != issuesCount) {
                logger.someIssuesFailedToParse()
            }

            IssuesLoadResult.Success(issues.map { IssueTemplate(it) })
        } catch (e: YouTrackClientException) {
            logger.externalServiceClientError(e,
                "failed to parse YT response. Does `--youtrackServer` URL match the one in your YT Domain Settings? Typically, it should end with /youtrack")

            IssuesLoadResult.Failed.messageOrUnknownException(e)
        } catch (e: Exception) {
            logger.generalError(e)

            IssuesLoadResult.Failed.messageOrUnknownException(e)
        }
    }
}
