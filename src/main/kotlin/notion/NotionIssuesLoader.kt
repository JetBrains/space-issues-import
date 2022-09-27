package com.jetbrains.space.import.notion

import com.jetbrains.space.import.common.*
import com.jetbrains.space.import.space.IssueTemplate
import com.petersamokhin.notionsdk.Notion
import com.petersamokhin.notionsdk.data.model.result.*
import com.petersamokhin.notionsdk.markdown.NotionMarkdownExporter
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.types.ExternalIssue

object NotionIssuesLoaderFactory {
    fun create(token: String): IssuesLoader =
        NotionIssuesLoader(token)
}

private class NotionIssuesLoader(token: String) : IssuesLoader {
    private val notion = Notion.fromToken(token, httpClient = createHttpClient())
    private val markdownExporter = NotionMarkdownExporter.create()

    override suspend fun load(params: IssuesLoader.Params): IssuesLoadResult {
        if (params !is IssuesLoader.Params.Notion)
            return IssuesLoadResult.Failed.wrongParams(NotionIssuesLoader::class)

        return try {
            val notionCards = getAllNotionCards(databaseId = params.databaseId, query = params.query.orEmpty())

            IssuesLoadResult.Success(
                notionCards.mapIndexed { cardIndex, card ->
                    val externalIssue = ExternalIssue(
                        summary = card.getTitle()
                            ?: return IssuesLoadResult.Failed("No property found in Notion database for title"),
                        description = card.getDescriptionAsMarkdown(),
                        status = params.statusProperty?.let { card.findProperty(it)?.value?.getTextValue(params.statusPropertyMappingType) }
                            .orEmpty(),
                        assignee = params.assigneeProperty?.let { card.findProperty(it)?.value?.getTextValue(params.assigneePropertyMappingType) },
                        externalId = card.id,
                        externalName = "Notion",
                        externalUrl = "https://notion.so/${card.id.replace("-", "")}",
                    )
                        .also { logger.info(it, cardIndex, notionCards.size) }

                    val tags = when (val property = params.tagProperty?.let { card.findProperty(it)?.value }) {
                        is NotionDatabaseProperty.Title -> when (params.tagPropertyMappingType) {
                            ProjectPropertyType.Id -> setOf(property.id)
                            ProjectPropertyType.Name -> setOf(property.text)
                            ProjectPropertyType.Email -> null
                        }

                        is NotionDatabaseProperty.Text -> when (params.tagPropertyMappingType) {
                            ProjectPropertyType.Id -> setOf(property.id)
                            ProjectPropertyType.Name -> setOf(property.text)
                            ProjectPropertyType.Email -> null
                        }

                        is NotionDatabaseProperty.Select -> when (params.tagPropertyMappingType) {
                            ProjectPropertyType.Id -> setOf(property.id)
                            ProjectPropertyType.Name -> property.selected?.name?.let(::setOf)
                            ProjectPropertyType.Email -> null
                        }

                        is NotionDatabaseProperty.MultiSelect -> when (params.tagPropertyMappingType) {
                            ProjectPropertyType.Id -> property.selected.map(NotionDatabaseProperty.Select.Option::id)
                            ProjectPropertyType.Name -> property.selected.map(NotionDatabaseProperty.Select.Option::name)
                            ProjectPropertyType.Email -> null
                        }?.toSet()?.takeIf(Set<String>::isNotEmpty)

                        else -> null
                    } ?: emptySet()

                    IssueTemplate(externalIssue, tags)
                }
            )
        } catch (e: Exception) {
            logger.externalServiceClientError(e, "failed to retrieve issues from Notion")
            IssuesLoadResult.Failed.messageOrUnknownException(e)
        }
    }

    private fun NotionDatabaseProperty.getTextValue(type: ProjectPropertyType): String? =
        when (this) {
            is NotionDatabaseProperty.Title -> text
            is NotionDatabaseProperty.Text -> text
            is NotionDatabaseProperty.Select -> when (type) {
                ProjectPropertyType.Id -> selected?.id
                ProjectPropertyType.Name -> selected?.name
                ProjectPropertyType.Email -> null
            }
            is NotionDatabaseProperty.MultiSelect -> selected.firstOrNull()?.let { option ->
                when (type) {
                    ProjectPropertyType.Id -> option.id
                    ProjectPropertyType.Name -> option.name
                    ProjectPropertyType.Email -> null
                }
            }
            is NotionDatabaseProperty.Email -> email
            is NotionDatabaseProperty.PhoneNumber -> phoneNumber
            is NotionDatabaseProperty.People -> people.firstOrNull()?.getTextValue(type)
            is NotionDatabaseProperty.CreatedBy -> createdBy.getTextValue(type)
            is NotionDatabaseProperty.LastEditedBy -> lastEditedBy.getTextValue(type)
            else -> null
        }

    private fun NotionDatabaseProperty.People.Person.getTextValue(type: ProjectPropertyType): String? =
        when (this) {
            is NotionDatabaseProperty.People.Person.User -> when (type) {
                ProjectPropertyType.Id -> id
                ProjectPropertyType.Name -> name
                ProjectPropertyType.Email -> email
            }
            is NotionDatabaseProperty.People.Person.Bot -> when (type) {
                ProjectPropertyType.Id -> id
                ProjectPropertyType.Name -> name
                ProjectPropertyType.Email -> null
            }
        }

    private fun NotionDatabaseRow.getTitle(): String? {
        val titleText = findProperty(ExternalProjectProperty.Id(CARD_TITLE_PROPERTY_ID))
            ?.let { property -> property.value as? NotionDatabaseProperty.Title }
            ?.text

        val titleEmoji = (icon as? NotionIcon.Emoji)?.emoji?.let { emoji -> "$emoji " }.orEmpty()

        return titleText?.let { "$titleEmoji$titleText" }
    }

    private fun NotionDatabaseRow.findProperty(property: ExternalProjectProperty): NotionDatabaseColumn? =
        columns.firstNotNullOfOrNull { (key, column) ->
            column.takeIf {
                when (property) {
                    is ExternalProjectProperty.Id -> column.value.id == property.id
                    is ExternalProjectProperty.Name -> key == property.name
                }
            }
        }

    private suspend fun getAllNotionCards(databaseId: String, query: String): List<NotionDatabaseRow> {
        return if (query.isEmpty()) {
            var lastResponse = notion.queryDatabase(databaseId)
            val result = lastResponse.results.toMutableList()

            while (lastResponse.hasMore && lastResponse.nextCursor != null) {
                lastResponse = notion.queryDatabase(databaseId, lastResponse.nextCursor)
                result += lastResponse.results
            }

            result
        } else {
            notion.queryDatabase(databaseId, query).results
        }
    }

    private suspend fun NotionDatabaseRow.getDescriptionAsMarkdown(): String =
        markdownExporter.exportRecursively(getAllBlocks(), notion = notion, depthLevel = 2)

    private suspend fun NotionDatabaseRow.getAllBlocks(): List<NotionBlock> {
        var lastResponse = notion.retrieveBlockChildren(id)
        val result = lastResponse.results.toMutableList()

        while (lastResponse.hasMore && lastResponse.nextCursor != null) {
            lastResponse = notion.retrieveBlockChildren(id, lastResponse.nextCursor)
            result += lastResponse.results
        }

        return result
    }

    private fun createHttpClient(): HttpClient =
        HttpClient(Apache) {
            engine {
                followRedirects = true
                socketTimeout = 60_000
                connectTimeout = 60_000
                connectionRequestTimeout = 60_000
            }
        }

    companion object {
        private const val CARD_TITLE_PROPERTY_ID = "title"
        private val logger = LoggerFactory.getLogger(NotionIssuesLoader::class.java)
    }
}