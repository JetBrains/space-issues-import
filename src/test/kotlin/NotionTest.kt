import com.jetbrains.space.import.common.*
import com.jetbrains.space.import.notion.NotionIssuesLoaderFactory
import io.ktor.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class NotionTestEnvironmentVariables : SpaceEnvironmentVariables() {
    val notionToken = EnvironmentVariableLoader.loadRequired("NOTION_TOKEN")
    val notionDatabaseId = EnvironmentVariableLoader.loadRequired("NOTION_DATABASE_ID")
    val notionQuery = EnvironmentVariableLoader.loadWithDefault("NOTION_QUERY", "")

    val notionAssigneePropertyName = EnvironmentVariableLoader.loadNullable("NOTION_ASSIGNEE_PROPERTY_NAME")
    val notionStatusPropertyName = EnvironmentVariableLoader.loadNullable("NOTION_STATUS_PROPERTY_NAME")
    val notionTagPropertyName = EnvironmentVariableLoader.loadNullable("NOTION_TAG_PROPERTY_NAME")
}

class NotionTest {
    private lateinit var notionLoader: IssuesLoader
    private lateinit var environmentVariables: NotionTestEnvironmentVariables

    @BeforeEach
    fun setUp() {
        environmentVariables = NotionTestEnvironmentVariables()
        notionLoader = NotionIssuesLoaderFactory.create(environmentVariables.notionToken)
    }

    @InternalAPI
    @Test
    fun `load issues from notion`()
        = testLoadingIssuesFromExternalService(environmentVariables, ImportSource.Notion) {
            notionLoader.load(
                IssuesLoader.Params.Notion(
                    query = environmentVariables.notionQuery,
                    databaseId = environmentVariables.notionDatabaseId,
                    assigneeProperty = environmentVariables.notionAssigneePropertyName?.let { ExternalProjectProperty.Name(it) },
                    statusProperty = environmentVariables.notionStatusPropertyName?.let { ExternalProjectProperty.Name(it) },
                    tagProperty = environmentVariables.notionTagPropertyName?.let { ExternalProjectProperty.Name(it) }
                )
            )
        }
}