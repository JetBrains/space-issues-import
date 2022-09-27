import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.IssuesLoadResult
import com.jetbrains.space.import.space.IssueTemplate
import com.jetbrains.space.import.space.SpaceUploaderImpl
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import space.jetbrains.api.runtime.types.ImportExistsPolicy
import space.jetbrains.api.runtime.types.ImportMissingPolicy
import space.jetbrains.api.runtime.types.ProjectIdentifier
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object EnvironmentVariableLoader {
    fun loadRequired(name: String) = System.getenv(name)
        ?: throw IllegalArgumentException("'$name' environment variable must be specified.")
    fun loadNullable(name: String): String? = System.getenv(name)
    fun loadWithDefault(name: String, default: String) = System.getenv(name) ?: default
    fun <T> loadWithDefault(name: String, default: T, transform: (String, String) -> T)
        = System.getenv(name)?.let { transform(name, it) } ?: default

    fun transformToBoolean(name: String, value: String): Boolean
        = value.lowercase().toBooleanStrictOrNull() ?: throw IllegalArgumentException("'$name' should be either 'true' or 'false'.")
}

open class SpaceEnvironmentVariables {
    val spaceServer = EnvironmentVariableLoader.loadWithDefault("SPACE_SERVER", "")
    val spaceToken = EnvironmentVariableLoader.loadWithDefault("SPACE_TOKEN", "")
    val spaceProjectKey = EnvironmentVariableLoader.loadWithDefault("SPACE_PROJECT_KEY", "")
    val tryImportIntoSpace = EnvironmentVariableLoader.loadWithDefault(
        "TRY_IMPORT_INTO_SPACE",
        true,
        EnvironmentVariableLoader::transformToBoolean
    )

    init {
        if (tryImportIntoSpace && "" in listOf(spaceServer, spaceToken, spaceProjectKey))
            throw IllegalArgumentException(
                "When 'IMPORT_INTO_SPACE' is 'true' you need to supply 'SPACE_SERVER', 'SPACE_TOKEN' and 'SPACE_PROJECT_KEY'."
            )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T> assertCast(value: Any) {
    contract {
        returns() implies (value is T)
    }
    assertTrue(value is T)
}

@OptIn(InternalAPI::class)
suspend fun tryImportingIssuesIntoSpace(
    environmentVariables: SpaceEnvironmentVariables,
    issues: List<IssueTemplate>,
    importSource: ImportSource
) = SpaceUploaderImpl()
        .upload(
            server = environmentVariables.spaceServer,
            token = environmentVariables.spaceToken,

            issues = issues,
            projectIdentifier = ProjectIdentifier.Key(environmentVariables.spaceProjectKey),
            importSource = importSource,

            assigneeMissingPolicy = ImportMissingPolicy.Skip,
            statusMissingPolicy = ImportMissingPolicy.Skip,
            onExistsPolicy = ImportExistsPolicy.Skip,

            dryRun = true,
            batchSize = 100,
            debug = true
        )

fun testLoadingIssuesFromExternalService(
    environmentVariables: SpaceEnvironmentVariables,
    importSource: ImportSource,
    loadIssues: suspend () -> IssuesLoadResult
) {
    assertDoesNotThrow {
        runBlocking {
            val issuesLoadResult = loadIssues()
            assertCast<IssuesLoadResult.Success>(issuesLoadResult)

            if (environmentVariables.tryImportIntoSpace)
                tryImportingIssuesIntoSpace(environmentVariables, issuesLoadResult.issues, importSource)
        }
    }
}
