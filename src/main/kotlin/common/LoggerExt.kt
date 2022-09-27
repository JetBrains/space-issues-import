package com.jetbrains.space.import.common

import org.slf4j.Logger
import space.jetbrains.api.runtime.types.ExternalIssue


fun Logger.createHttpClientLogger() = object : io.ktor.client.features.logging.Logger {
    override fun log(message: String) {
        info("HTTP Client: $message")
    }
}

fun Logger.info(issue: ExternalIssue) {
    with(issue) {
        info("id         \t${externalId}")
        info("status     \t${status}")
        info("assignee   \t${assignee}")
        info("summary    \t${summary}")
        info("description\t${description?.take(64)?.lines()?.joinToString(" ")}...")
        info("---")
    }
}

fun Logger.info(issue: ExternalIssue, issueIndex: Int, issuesCount: Int) {
    info("issue ${issueIndex + 1} / $issuesCount")
    info(issue)
}


fun Logger.failedToParseIssue(issueIndex: Int, issuesCount: Int, source: String) {
    error("failed to parse issue ${issueIndex + 1} / $issuesCount from $source")
}

fun Logger.someIssuesFailedToParse() {
    error("some issues failed to parse, report the problem here: https://github.com/JetBrains/space-issues-import/issues/new")
}

fun Logger.externalServiceClientError(e: Exception, message: String) {
    error(message)
    error("original error message: $e")
    error("if the problem still persists, report it here: https://github.com/JetBrains/space-issues-import/issues/new")
}

fun Logger.generalError(e: Exception) {
    error(e.toString())
    error("report the problem here: https://github.com/JetBrains/space-issues-import/issues/new")
}
