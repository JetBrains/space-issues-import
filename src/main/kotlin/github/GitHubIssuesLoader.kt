package com.jetbrains.space.import.github

import com.jetbrains.space.import.common.*
import com.jetbrains.space.import.space.IssueTemplate
import org.kohsuke.github.GHDirection
import org.kohsuke.github.GHIssueQueryBuilder
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHubBuilder
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.types.ExternalIssue


object GitHubIssuesLoaderFactory {
    fun create(authorization: GitHubAuthorization): IssuesLoader
        = GitHubIssuesLoader(authorization)
}

private class GitHubIssuesLoader(
    private val authorization: GitHubAuthorization
) : IssuesLoader {

    private val gitHub = with(GitHubBuilder()) {
        when (authorization) {
            is GitHubAuthorization.OAuth -> when (authorization.login) {
                null -> withOAuthToken(authorization.token)
                else -> withOAuthToken(authorization.token, authorization.login)
            }
            is GitHubAuthorization.Jwt -> withJwtToken(authorization.token)
            is GitHubAuthorization.AppInstallationToken -> withAppInstallationToken(authorization.token)
            is GitHubAuthorization.None -> Unit
        }
        build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitHubIssuesLoader::class.java)
    }

    override suspend fun load(params: IssuesLoader.Params) : IssuesLoadResult {
        if (params !is IssuesLoader.Params.GitHub)
            return IssuesLoadResult.Failed.wrongParams(GitHubIssuesLoader::class)

        val issues = try {
            gitHub
                .getRepository("${params.owner}/${params.repository}")
                .queryIssues()
                .state(GHIssueState.ALL)
                .pageSize(defaultBatchSize)
                .sort(GHIssueQueryBuilder.Sort.CREATED)
                .direction(GHDirection.ASC)
                .list()
                .filter { it.pullRequest == null }
        } catch (e: Exception) {
            logger.externalServiceClientError(e, "failed to retrieve issues from GitHub")
            return IssuesLoadResult.Failed.messageOrUnknownException(e)
        }

        val issuesCount = issues.count()
        val result = issues.mapIndexedNotNull { issueIndex, it ->
            try {
                val title = it.title; val state = it.state; val number = it.number
                requireNotNull(title); requireNotNull(state)

                ExternalIssue(
                    summary = title,
                    description = it.body,
                    status = state.name,
                    assignee = it.assignee?.login,
                    externalId = number.toString(),
                    externalName = title,
                    externalUrl = it.htmlUrl.toString()
                )
                    .also { logger.info(it, issueIndex, issuesCount) }
                    .let { IssueTemplate(it) }
            } catch (e: Exception) {
                logger.failedToParseIssue(issueIndex, issuesCount, "GitHub")
                null
            }
        }

        if (result.count() != issuesCount) logger.someIssuesFailedToParse()

        return if (result.isNotEmpty() || issuesCount == 0) IssuesLoadResult.Success(result)
        else IssuesLoadResult.Failed("couldn't parse any issues")
    }
}

sealed interface GitHubAuthorization {
    class OAuth(val token: String, val login: String? = null): GitHubAuthorization
    class Jwt(val token: String): GitHubAuthorization
    class AppInstallationToken(val token: String): GitHubAuthorization
    object None : GitHubAuthorization
}
