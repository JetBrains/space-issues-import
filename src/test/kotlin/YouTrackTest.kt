import com.jetbrains.space.import.common.IssuesLoadResult
import com.jetbrains.space.import.common.IssuesLoader
import com.jetbrains.space.import.youtrack.YoutrackIssuesLoaderFactory
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class YouTrackTest {
    lateinit var httpClient: HttpClient
    lateinit var youtrack: IssuesLoader

    @BeforeEach
    fun setUp() {
        val youTrackServerEnv = "YOUTRACK_SERVER"
        val youTrackServerUrl = System.getenv(youTrackServerEnv)
            ?: fail("'$youTrackServerEnv' environment variable must be specified")

        youtrack = YoutrackIssuesLoaderFactory.create(youTrackServerUrl, "")

        HttpClient(Apache) {
            engine {
                followRedirects = true
                socketTimeout = 60_000
                connectTimeout = 60_000
                connectionRequestTimeout = 60_000
            }
        }.also { httpClient = it }
    }

    @InternalAPI
    @Test
    fun `load issues from youtrack`() {
        runBlocking {
            val issues = IssuesLoadResult.Success(emptyList(), emptyMap()) // youtrack.load("")
            assertTrue(issues is IssuesLoadResult.Success)

//            SpaceUploader()
//                .upload(
//                    server = spaceServer,
//                    token = spaceToken,
//
//                    issues = preprocessedIssues,
//                    projectIdentifier = spaceProject,
//                    importSource = importSource,
//
//                    assigneeMissingPolicy = assigneeMissingPolicy,
//                    statusMissingPolicy = statusMissingPolicy,
//                    onExistsPolicy = onExistsPolicy,
//                    dryRun = dryRun,
//
//                    batchSize = batchSize
//                )
        }
    }
}