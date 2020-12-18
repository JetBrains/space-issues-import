package com.jetbrains.space.import.jira

import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import com.jetbrains.space.import.common.IssuesLoadResult
import com.jetbrains.space.import.common.IssuesLoader
import io.atlassian.util.concurrent.Promise
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import space.jetbrains.api.runtime.types.ExternalIssue
import java.net.URI
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    override suspend fun load(query: String): IssuesLoadResult {
        return try {
            migrate(query)
        } catch (e: Exception) {
            LOG.error("Error whilst migrating issues", e)
            IssuesLoadResult.Failed(e.message ?: "Unknown expection")
        }
    }

    private suspend fun migrate(query: String): IssuesLoadResult {
        var total = 0
        var current = 0
        val allIssues = mutableListOf<ExternalIssue>()
        do {
            val search = client.searchClient.searchJql(query, 50, current, null).await()
            total = search.total
            val issues = search.issues.map {
                ExternalIssue(
                    summary = it.summary,
                    description = it.description,
                    assignee = it.assignee?.displayName ?: "",
                    status = it.status.statusCategory.key,
                    externalId = it.id.toString(),
                    externalName = it.key,
                    externalUrl = "$jiraUrl/secure/RapidBoard.jspa?projectKey=${it.project.key}&selectedIssue=${it.key}"
                )
            }
            current += issues.size
            allIssues.addAll(issues)
        } while(current < total)

        return IssuesLoadResult.Success(allIssues.toList())
    }

    companion object {
        val LOG = KotlinLogging.logger { }
    }
}

suspend fun <T> Promise<T>.await(): T {
    if (isDone) {
        try {
            @Suppress("UNCHECKED_CAST", "BlockingMethodInNonBlockingContext")
            return get() as T
        } catch (e: ExecutionException) {
            throw e.cause ?: e // unwrap original cause from ExecutionException
        }
    }
    // slow path -- suspend
    return suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        done { cont.resume(it) }
        fail { cont.resumeWithException(it) }
        cont.invokeOnCancellation {
            (this as? Promise<T>)?.cancel(false)
        }
    }
}
