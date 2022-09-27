package com.jetbrains.space.import.jira

import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import com.jetbrains.space.import.common.*
import com.jetbrains.space.import.space.IssueTemplate
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.types.ExternalIssue
import java.net.URI

object JiraIssuesLoaderFactory {
    fun create(jiraUrl: String, username: String?, password: String?): IssuesLoader =
        JiraIssuesLoader(jiraUrl, username, password)
}

private class JiraIssuesLoader(private val jiraUrl: String, username: String?, password: String?) :
    IssuesLoader {

    private val client: JiraRestClient

    init {
        val factory = AsynchronousJiraRestClientFactory()
        val authHandler = if (username == null || password == null) {
            AnonymousAuthenticationHandler()
        } else {
            BasicHttpAuthenticationHandler(username, password)
        }

        client = factory.createWithAuthenticationHandler(URI.create(jiraUrl), authHandler)
    }

    override suspend fun load(params: IssuesLoader.Params): IssuesLoadResult {
        if (params !is IssuesLoader.Params.Jira)
            return IssuesLoadResult.Failed.wrongParams(JiraIssuesLoader::class)

        return try {
            migrate(params.query.orEmpty())
        } catch (e: Exception) {
            logger.externalServiceClientError(e, "failed to retrieve issues from Jira")
            IssuesLoadResult.Failed.messageOrUnknownException(e)
        }
    }

    private fun migrate(query: String): IssuesLoadResult {
        var total = 0
        var current = 0
        val allIssues = mutableListOf<ExternalIssue>()
        do {
            client.searchClient
                .searchJql(query, defaultBatchSize, current, null)
                .done { search ->
                    total = search.total
                    val issues = search.issues.mapIndexed { issueIndex, it ->
                        ExternalIssue(
                            summary = it.summary,
                            description = it.description,
                            assignee = it.assignee?.displayName ?: "",
                            status = it.status.statusCategory.key,
                            externalId = it.id.toString(),
                            externalName = it.key,
                            externalUrl = "$jiraUrl/secure/RapidBoard.jspa?projectKey=${it.project.key}&selectedIssue=${it.key}"
                        )
                            .also { logger.info(it, current + issueIndex, total) }
                    }
                    current += issues.size
                    allIssues.addAll(issues)
                }
                .fail { e ->
                    throw e.cause ?: e // unwrap original cause from ExecutionException
                }
                .claim()
        } while (current < total)

        return IssuesLoadResult.Success(allIssues.map { IssueTemplate(it) })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JiraIssuesLoader::class.java)
    }
}
