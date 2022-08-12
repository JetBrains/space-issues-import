fun Job.buildAndRunTests() {
    gradlew("openjdk:11", "test", "--info") {
        env["JIRA_SERVER"] = Params("issues-import-automated-tests-jira-server")
        env["JIRA_TOKEN"] = Secrets("issues-import-automated-tests-jira-token")
        env["JIRA_USER"] = Params("issues-import-automated-tests-jira-user")
        env["NOTION_ASSIGNEE_PROPERTY_NAME"] = Params("issues-import-automated-tests-notion-assignee-property")
        env["NOTION_DATABASE_ID"] = Params("issues-import-automated-tests-notion-database-id")
        env["NOTION_STATUS_PROPERTY_NAME"] = Params("issues-import-automated-tests-notion-status-property")
        env["NOTION_TOKEN"] = Secrets("issues-import-automated-tests-notion-token")
        env["TRY_IMPORT_INTO_SPACE"] = Params("issues-import-automated-tests-import-into-space")
        env["YOUTRACK_SERVER"] = Params("issues-import-automated-tests-youtrack-server")
        env["YOUTRACK_TOKEN"] = Secrets("issues-import-automated-tests-youtrack-token")
    }
}

job("Build and run tests") {
    startOn {
        gitPush {
            enabled = true
            branchFilter {
                +"refs/heads/*"
                -"refs/heads/main"
            }
        }
        schedule { cron("0 8 * * *") }
    }

    buildAndRunTests()
}

job("Build, run tests and push to public.jetbrains.space registry (latest)") {
    startOn {
        gitPush {
            branchFilter = "refs/heads/main"
        }
    }

    buildAndRunTests()

    docker("Push to public.jetbrains.space registry") {
        env["REGISTRY_USER"] = Secrets("public-jetbrains-space-issues-import-publisher-client-id")
        env["REGISTRY_TOKEN"] = Secrets("public-jetbrains-space-issues-import-publisher-token")

        beforeBuildScript {
            content = """
                B64_AUTH=${'$'}(echo -n ${'$'}REGISTRY_USER:${'$'}REGISTRY_TOKEN | base64 -w 0)
                echo "{\"auths\":{\"public.registry.jetbrains.space\":{\"auth\":\"${'$'}B64_AUTH\"}}}" > ${'$'}DOCKER_CONFIG/config.json
            """.trimIndent()
        }

        build()

        push("public.registry.jetbrains.space/p/space/containers/space-issues-import")
    }
}
