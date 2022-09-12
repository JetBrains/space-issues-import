package com.jetbrains.space.import

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import space.jetbrains.api.runtime.types.ImportExistsPolicy
import space.jetbrains.api.runtime.types.ImportMissingPolicy
import space.jetbrains.api.runtime.types.ProjectIdentifier

data class CommandLineArgs(private val parser: ArgParser) {
    private val mappingSeparator = "::"

    // Jira

    val jiraServer by parser.storing(
        "--jiraServer",
        help = "The URL of the Jira server that you want to import issues from"
    ).default(null)

    val jiraQuery by parser.storing(
        "--jiraQuery",
        help = "A JQL query that selects the Jira issues you want to import"
    ).default(null)

    val jiraUser by parser.storing(
        "--jiraUser",
        help = "An optional user name to use to login to Jira"
    ).default(null)

    val jiraPassword by parser.storing(
        "--jiraPassword",
        help = "An optional password to use to login to Jira"
    ).default(null)

    // YouTrack

    val youtrackServer by parser.storing(
        "--youtrackServer",
        help = "The URL of the YouTrack server that you want to import issues from."
    ).default(null)

    val youtrackQuery by parser.storing(
        "--youtrackQuery",
        help = "A query that selects the YouTrack issues that you want to import."
    ).default(null)

    val youtrackToken by parser.storing(
        "--youtrackToken",
        help = "An optional permanent token that grants access to the YouTrack server for a specific user account. " +
                "If not specified, issue data is retrieved according to the access rights that are available to the guest account."
    ).default(null)

    // Space

    val spaceServer by parser.storing(
        "--spaceServer",
        help = "The URL of the Space instance that you want to import into."
    )

    val spaceToken by parser.storing(
        "--spaceToken",
        help = "A personal token for a Space account that has the Import Issues permission."
    )

    val spaceProject by parser.storing(
            "--spaceProject",
            help = "The key or ID of a project in Space into which you want to import issues. For example, key${mappingSeparator}ABC or id${mappingSeparator}42.",
            transform = {
                val (identifierType, identifier) = parseMapping(this)
                when (identifierType.toLowerCase()) {
                    "key" -> ProjectIdentifier.Key(identifier)
                    "id" -> ProjectIdentifier.Key(identifier)
                    else -> throw SystemExitException("only key::value or id::value are allowed for --spaceProject as identifier", 2)
                }
            }
    )

    // Space /import API arguments

    val importSource by parser.storing(
        "--importSource",
        help = "The name of the import source. Default: External."
    ).default("External")

    val dryRun by parser.flagging(
            "--dryRun",
            help = "Runs the import script without actually creating issues."
    )

    val onExistsPolicy by parser.mapping(
            "--updateExistingIssues" to ImportExistsPolicy.Update,
            "--skipExistingIssues" to ImportExistsPolicy.Skip,
            help = "Tells Space how to process issues when their external IDs matches previously imported issues in Space. Default: skipExistingIssues."
    ).default(ImportExistsPolicy.Skip)

    val statusMissingPolicy by parser.mapping(
            "--replaceMissingStatus" to ImportMissingPolicy.ReplaceWithDefault,
            "--skipMissingStatus" to ImportMissingPolicy.Skip,
            help = "Tells Space how to handle issues when the value for the status field does not exist. `replaceMissingStatus` sets it to the first unresolved status. Default: `skipMissingStatus`."
    ).default(ImportMissingPolicy.Skip)

    val assigneeMissingPolicy by parser.mapping(
            "--replaceMissingAssignee" to ImportMissingPolicy.ReplaceWithDefault,
            "--skipMissingAssignee" to ImportMissingPolicy.Skip,
            help = "Tells Space how to handle issues when the value for the assignee field does not exist. `replaceMissingAssignee` sets it to `unassigned`. Default: `skipMissingAssignee`."
    ).default(ImportMissingPolicy.Skip)

    // add-ons

    val assigneeMapping by parser.adding(
            "-a", "--assignee",
            help = "Maps the assignee in the external system to a member profile in Space. For example, leonid.tolstoy${mappingSeparator}leo.tolstoy.",
            transform = { parseMapping(this) }
    ).default(emptyList())

    val statusMapping by parser.adding(
            "-s", "--status",
        help = "Maps an issue status in the external system to an issue status in Space. For example, in-progress${mappingSeparator}In Progress.",
        transform = { parseMapping(this) }
    ).default(emptyList())

    val batchSize by parser.storing(
        "--batchSize",
        help = "The size of a batch with issues being sent to Space per request. Default: 50.",
        transform = { toInt() }
    ).default(50)

    val debug by parser.flagging(
        "--debug",
        help = "Runs the import script in debug mode."
    )

    private fun parseMapping(arg: String, separator: String = mappingSeparator): Pair<String, String> {
        val mapping = arg.split(separator)
        if (mapping.size != 2)
            throw SystemExitException("mapping format is wrong for argument: $arg", 2)

        return mapping.first() to mapping.last()
    }
}
