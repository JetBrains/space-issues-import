import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.IssuesLoader
import com.jetbrains.space.import.youtrack.YoutrackIssuesLoaderFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class YouTrackTestEnvironmentVariables : SpaceEnvironmentVariables() {
    val youTrackServer = EnvironmentVariableLoader.loadRequired("YOUTRACK_SERVER")
    val youTrackToken = EnvironmentVariableLoader.loadNullable("YOUTRACK_TOKEN")
    val youTrackQuery = EnvironmentVariableLoader.loadWithDefault("YOUTRACK_QUERY", "")
}

class YouTrackTest {
    private lateinit var youTrackLoader: IssuesLoader
    private lateinit var environmentVariables: YouTrackTestEnvironmentVariables

    @BeforeEach
    fun setUp() {
        environmentVariables = YouTrackTestEnvironmentVariables()
        youTrackLoader = YoutrackIssuesLoaderFactory.create(
            serverUrl = environmentVariables.youTrackServer,
            token = environmentVariables.youTrackToken
        )
    }

    @Test
    fun `load issues from youtrack`()
        = testLoadingIssuesFromExternalService(environmentVariables, ImportSource.YouTrack) {
            youTrackLoader.load(IssuesLoader.Params.YouTrack(environmentVariables.youTrackQuery))
        }
}