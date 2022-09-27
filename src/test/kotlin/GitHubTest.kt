import com.jetbrains.space.import.CommandLineArgs
import com.jetbrains.space.import.common.ImportSource
import com.jetbrains.space.import.common.IssuesLoader
import com.jetbrains.space.import.github.GitHubAuthorization
import com.jetbrains.space.import.github.GitHubIssuesLoaderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class GitHubTestEnvironmentVariables : SpaceEnvironmentVariables() {
    val gitHubOAuthToken = EnvironmentVariableLoader.loadNullable("GITHUB_OAUTH_TOKEN")
    val gitHubRepositoryOwner = EnvironmentVariableLoader.loadRequired("GITHUB_REPO_OWNER")
    val gitHubRepository = EnvironmentVariableLoader.loadRequired("GITHUB_REPO")
}

class GitHubTest {
    private lateinit var gitHubLoader: IssuesLoader
    private lateinit var environmentVariables: GitHubTestEnvironmentVariables

    @BeforeEach
    fun setUp() {
        environmentVariables = GitHubTestEnvironmentVariables()
        gitHubLoader =  GitHubIssuesLoaderFactory.create(
            environmentVariables.gitHubOAuthToken?.let { GitHubAuthorization.OAuth(it) }
                ?: GitHubAuthorization.None
        )
    }

    @Test
    fun `load public issues from github`()
            = testLoadingIssuesFromExternalService(environmentVariables, ImportSource.GitHub) {
        gitHubLoader.load(
            IssuesLoader.Params.GitHub(
                owner = "JetBrains",
                repository = "space-issues-import"
            )
        )
    }

    @Test
    fun `load private issues from github`()
            = testLoadingIssuesFromExternalService(environmentVariables, ImportSource.GitHub) {
        gitHubLoader.load(
            IssuesLoader.Params.GitHub(
                owner = environmentVariables.gitHubRepositoryOwner,
                repository = environmentVariables.gitHubRepository
            )
        )
    }

    @Test
    fun `github authorization cli parsing`() {
        val token = "token_contains::t"
        val login = "name"

        // OAuth

        val oauthAuthorization = CommandLineArgs.parseGitHubAuthorization("oauth::$token")
        assertCast<GitHubAuthorization.OAuth>(oauthAuthorization)
        assertEquals(token, oauthAuthorization.token)
        assertNull(oauthAuthorization.login)

        // OAuth with login

        val oauthLoginAuthorization = CommandLineArgs.parseGitHubAuthorization("oauth_with_login::$login::$token")
        assertCast<GitHubAuthorization.OAuth>(oauthLoginAuthorization)
        assertEquals(token, oauthLoginAuthorization.token)
        assertEquals(login, oauthLoginAuthorization.login)

        // JWT

        val jwtAuthorization = CommandLineArgs.parseGitHubAuthorization("jwt::$token")
        assertCast<GitHubAuthorization.Jwt>(jwtAuthorization)
        assertEquals(token, jwtAuthorization.token)

        // App Installation Token

        val aitAuthorization = CommandLineArgs.parseGitHubAuthorization("ait::$token")
        assertCast<GitHubAuthorization.AppInstallationToken>(aitAuthorization)
        assertEquals(token, aitAuthorization.token)
    }
}