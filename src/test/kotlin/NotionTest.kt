import com.jetbrains.space.import.CommandLineArgs
import com.jetbrains.space.import.common.ExternalProjectProperty
import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.IssuesLoader
import com.jetbrains.space.import.common.ProjectPropertyType
import com.jetbrains.space.import.notion.NotionIssuesLoaderFactory
import io.ktor.util.*
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun `notion property cli parsing`() {
        val id = "identifier"
        val nameProperty = CommandLineArgs.parseNotionProperty("name::$id", "property")
        assertCast<ExternalProjectProperty.Name>(nameProperty)
        assertEquals(id, nameProperty.name)

        val idProperty = CommandLineArgs.parseNotionProperty("id::$id", "property")
        assertCast<ExternalProjectProperty.Id>(idProperty)
        assertEquals(id, idProperty.id)
    }

    @Test
    fun `notion property mapping type cli parsing`() {
        val nameType = CommandLineArgs.parseNotionPropertyMappingType("Name")
        assertEquals(ProjectPropertyType.Name, nameType)

        val idType = CommandLineArgs.parseNotionPropertyMappingType("Id")
        assertEquals(ProjectPropertyType.Id, idType)

        val emailType = CommandLineArgs.parseNotionPropertyMappingType("Email")
        assertEquals(ProjectPropertyType.Email, emailType)
    }
}