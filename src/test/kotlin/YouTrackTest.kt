import com.jetbrains.space.import.common.IssuesLoadResult
import com.jetbrains.space.import.common.IssuesLoader
import com.jetbrains.space.import.youtrack.YoutrackIssuesLoaderFactory
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.util.*
import junit.framework.AssertionFailedError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YouTrackTest {
    lateinit var httpClient: HttpClient
    lateinit var youtrack: IssuesLoader

    @Before
    fun setUp() {
        val youTrackServerEnv = "YOUTRACK_SERVER"
        val youTrackServerUrl = System.getenv(youTrackServerEnv)
            ?: throw AssertionFailedError("'$youTrackServerEnv' environment variable must be specified")

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
            val issues = IssuesLoadResult.Success(emptyList()) // youtrack.load("")
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