import com.jetbrains.space.import.CommandLineArgs
import com.jetbrains.space.import.common.*
import com.jetbrains.space.import.main
import com.jetbrains.space.import.space.IssueTemplate
import com.jetbrains.space.import.space.SpaceUploader
import com.xenomachina.argparser.SystemExitException
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import space.jetbrains.api.runtime.types.*
import kotlin.properties.Delegates

class CLITest {
    companion object {
        val statusesMapping = mapOf(
            "ongoing" to "inprogress",
            "notstarted" to "open"
        )
        val assigneesMapping = mapOf(
            "assignee1" to "john1",
            "assignee2" to "john2"
        )

        const val spaceServer = "http://localhost:8000"
        const val spaceToken = "token"
        const val spaceProject = "TEST"
    }

    @Test
    fun `mappings parsing and replacement`() {

        // mapping parsing

        assertThrows(SystemExitException::class.java) {
            CommandLineArgs.parseMapping("test:23")
        }
        assertThrows(SystemExitException::class.java) {
            CommandLineArgs.parseMapping("test::23::34")
        }
        assertDoesNotThrow {
            CommandLineArgs.parseMapping("test::23::34", canContainMultipleSeparators = true)
        }

        // mapping replacement

        val args = """
            --importSource GitHub --spaceServer $spaceServer --spaceToken $spaceToken --spaceProject key::$spaceProject
            ${assigneesMapping.toList().joinToString(" ") { "-a ${it.first}::${it.second}" }}
            ${statusesMapping.toList().joinToString(" ") { "-s ${it.first}::${it.second}" }}
        """.trimIndent().replace('\n', ' ')

        val spaceUploader = TestSpaceUploader()
        val initialIssues = testExecution(args, spaceUploader)

        spaceUploader.assert {
            val finalIssues = issues
            val projectIdentifier = projectIdentifier
            assertCast<ProjectIdentifier.Key>(projectIdentifier)
            assertEquals(spaceProject, projectIdentifier.key)

            assertEquals(2, finalIssues.size)
            (initialIssues zip finalIssues).forEach { (initialIssue, finalIssue) ->
                val expectedFinalAssignee = assigneesMapping[initialIssue.externalIssue.assignee]
                assertEquals(expectedFinalAssignee, finalIssue.externalIssue.assignee)

                val expectedFinalStatus = statusesMapping[initialIssue.externalIssue.status]
                assertEquals(expectedFinalStatus, finalIssue.externalIssue.status)
            }
        }
    }

    @Test
    fun `import issues API parameters`() {
        val importSource = "GitHub"
        val args1 = """
            --importSource $importSource --spaceServer $spaceServer --spaceToken $spaceToken --spaceProject key::$spaceProject
        """.trimIndent().replace('\n', ' ')

        val spaceUploader1 = TestSpaceUploader()
        testExecution(args1, spaceUploader1)

        spaceUploader1.assert {
            val projectIdentifier = projectIdentifier
            assertCast<ProjectIdentifier.Key>(projectIdentifier)
            assertEquals(spaceProject, projectIdentifier.key)

            assertEquals(false, dryRun)
            assertEquals(ImportMissingPolicy.Skip, statusMissingPolicy)
            assertEquals(ImportMissingPolicy.Skip, assigneeMissingPolicy)
            assertEquals(ImportExistsPolicy.Skip, onExistsPolicy)
        }

        val args2 = """
            --importSource $importSource --spaceServer $spaceServer --spaceToken $spaceToken --spaceProject id::$spaceProject
            --dryRun --replaceMissingStatus --replaceMissingAssignee --updateExistingIssues
        """.trimIndent().replace('\n', ' ')

        val spaceUploader2 = TestSpaceUploader()
        testExecution(args2, spaceUploader2)

        spaceUploader2.assert {
            val projectIdentifier = projectIdentifier
            assertCast<ProjectIdentifier.Id>(projectIdentifier)
            assertEquals(spaceProject, projectIdentifier.id)

            assertEquals(true, dryRun)
            assertEquals(ImportMissingPolicy.ReplaceWithDefault, statusMissingPolicy)
            assertEquals(ImportMissingPolicy.ReplaceWithDefault, assigneeMissingPolicy)
            assertEquals(ImportExistsPolicy.Update, onExistsPolicy)
        }
    }

    @OptIn(InternalAPI::class)
    private fun testExecution(args: String, spaceUploader: SpaceUploader): List<IssueTemplate> {
        val issuesLoader = TestIssuesLoader()
        val params = IssuesLoader.Params.GitHub("owner", "repo")
        main(args.split(Regex(" +")).toTypedArray(), spaceUploader) { issuesLoader to params }

        val initialResult = runBlocking {
            issuesLoader.load(params)
        }
        assertCast<IssuesLoadResult.Success>(initialResult)
        return initialResult.issues
    }
}

private class TestIssuesLoader : IssuesLoader {
    override suspend fun load(params: IssuesLoader.Params): IssuesLoadResult {
        val initialStatuses = CLITest.statusesMapping.keys.toList()
        val initialAssignees = CLITest.assigneesMapping.keys.toList()

        assertEquals(initialStatuses.size, initialAssignees.size)
        return IssuesLoadResult.Success(initialStatuses.indices.map { id ->
            ExternalIssue(
                summary = "#$id",
                description = "",
                status = initialStatuses[id],
                assignee = initialAssignees[id],
                externalId = "#$id",
                externalName = "",
                externalUrl = ""
            ).let { issue ->
                IssueTemplate(issue)
            }
        })
    }
}

private class TestSpaceUploader: SpaceUploader {
    lateinit var server: String
    lateinit var token: String
    lateinit var issues: List<IssueTemplate>
    lateinit var projectIdentifier: ProjectIdentifier
    lateinit var importSource: ImportSource
    lateinit var assigneeMissingPolicy: ImportMissingPolicy
    lateinit var statusMissingPolicy: ImportMissingPolicy
    lateinit var onExistsPolicy: ImportExistsPolicy
    var dryRun by Delegates.notNull<Boolean>()
    var batchSize by Delegates.notNull<Int>()
    var debug by Delegates.notNull<Boolean>()

    @InternalAPI
    override suspend fun upload(
        server: String,
        token: String,
        issues: List<IssueTemplate>,
        projectIdentifier: ProjectIdentifier,
        importSource: ImportSource,
        assigneeMissingPolicy: ImportMissingPolicy,
        statusMissingPolicy: ImportMissingPolicy,
        onExistsPolicy: ImportExistsPolicy,
        dryRun: Boolean,
        batchSize: Int,
        debug: Boolean,
        tagPropertyMappingType: ProjectPropertyType?
    ): List<IssueImportResult> {
        this.server = server
        this.token = token
        this.issues = issues
        this.projectIdentifier = projectIdentifier
        this.importSource = importSource
        this.assigneeMissingPolicy = assigneeMissingPolicy
        this.statusMissingPolicy = statusMissingPolicy
        this.onExistsPolicy = onExistsPolicy
        this.dryRun = dryRun
        this.batchSize = batchSize
        this.debug = debug
        return emptyList()
    }

    fun assert(assertions: TestSpaceUploader.() -> Unit) {
        assertEquals(CLITest.spaceServer, server)
        assertEquals(CLITest.spaceToken, token)
        assertions()
    }
}
