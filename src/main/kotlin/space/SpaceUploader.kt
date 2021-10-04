package com.jetbrains.space.import.space

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.logging.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.SpaceHttpClient
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.*
import space.jetbrains.api.runtime.withPermanentToken

class SpaceUploader {
    companion object {
        private val logger = LoggerFactory.getLogger(SpaceUploader::class.java)
    }

    @InternalAPI
    suspend fun upload(
        server: String,
        token: String,
        issues: List<ExternalIssue>,
        projectIdentifier: ProjectIdentifier,
        importSource: String,
        assigneeMissingPolicy: ImportMissingPolicy,
        statusMissingPolicy: ImportMissingPolicy,
        onExistsPolicy: ImportExistsPolicy,
        dryRun: Boolean,
        batchSize: Int,
        debug: Boolean,
    ) {
        val httpClient = createHttpClient()
            .let {
                if (debug) {
                    it.config {
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                    }
                } else {
                    it
                }
            }

        val spaceClient = SpaceHttpClient(httpClient).withPermanentToken(token, server)

        if (dryRun) logger.info("[DRY RUN]")

        issues.chunked(batchSize).forEach { issuesBatched ->
            val response = spaceClient.projects.planning.issues.importIssues(
                project = projectIdentifier,
                metadata = ImportMetadata(importSource),
                issues = issuesBatched,
                assigneeMissingPolicy = assigneeMissingPolicy,
                statusMissingPolicy = statusMissingPolicy,
                onExistsPolicy = onExistsPolicy,
                dryRun = dryRun
            )
            logger.info(response.message)
        }
    }

    private fun createHttpClient(): HttpClient {
        return HttpClient(Apache) {
            engine {
                followRedirects = true
                socketTimeout = 60_000
                connectTimeout = 60_000
                connectionRequestTimeout = 60_000
            }
        }
    }
}
