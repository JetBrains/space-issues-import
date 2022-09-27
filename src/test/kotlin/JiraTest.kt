import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.IssuesLoader
import com.jetbrains.space.import.jira.JiraIssuesLoaderFactory
import io.ktor.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class JiraTestEnvironmentVariables : SpaceEnvironmentVariables() {
    val jiraServer = EnvironmentVariableLoader.loadRequired("JIRA_SERVER")
    val jiraUser = EnvironmentVariableLoader.loadRequired("JIRA_USER")
    val jiraToken = EnvironmentVariableLoader.loadNullable("JIRA_TOKEN")
    val jiraQuery = EnvironmentVariableLoader.loadWithDefault("JIRA_QUERY", "")
}

class JiraTest {
    private lateinit var jiraLoader: IssuesLoader
    private lateinit var environmentVariables: JiraTestEnvironmentVariables

    @BeforeEach
    fun setUp() {
        environmentVariables = JiraTestEnvironmentVariables()
        jiraLoader = JiraIssuesLoaderFactory.create(
            jiraUrl = environmentVariables.jiraServer,
            username = environmentVariables.jiraUser,
            password = environmentVariables.jiraToken
        )
    }

    @InternalAPI
    @Test
    fun `load issues from jira`()
        = testLoadingIssuesFromExternalService(environmentVariables, ImportSource.Jira) {
            jiraLoader.load(IssuesLoader.Params.Jira(environmentVariables.jiraQuery))
        }
}