job("Build and run tests") {
    startOn {
        gitPush { enabled = true }
        schedule { cron("0 8 * * *") }
    }

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
