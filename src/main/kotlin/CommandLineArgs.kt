package com.jetbrains.space.import

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import space.jetbrains.api.runtime.types.ImportExistsPolicy
import space.jetbrains.api.runtime.types.ImportMissingPolicy
import space.jetbrains.api.runtime.types.ProjectIdentifier

data class CommandLineArgs(private val parser: ArgParser) {
    private val mappingSeparator = "::"

    val youtrackServer by parser.storing(
        "--youtrackServer",
        help = "URL of a YouTrack server to import issues from"
    )

    val youtrackQuery by parser.storing(
        "--youtrackQuery",
        help = "a query to select issues from YouTrack server"
    )

    val youtrackToken by parser.storing(
        "--youtrackToken",
        help = "Optional YouTrack token to access server. Will fetch issues as a guest otherwise"
    ).default(null)

    val spaceServer by parser.storing(
        "--spaceServer",
        help = "URL of a Space server to operate on"
    )

    val spaceToken by parser.storing(
        "--spaceToken",
        help = "Personal Token with Import Issues permission of a Space server"
    )

    val spaceProject by parser.storing(
        "--spaceProject",
        help = "The key or ID of a project in Space to import issues into. E.g. key${mappingSeparator}ABC or id${mappingSeparator}42",
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
        help = "Import source name"
    ).default("YouTrack")

    val dryRun by parser.flagging(
        "--dryRun",
        help = "tell Space to run import without actually creating issues"
    )

    val onExistsPolicy by parser.mapping(
        "--updateExistingIssues" to ImportExistsPolicy.Update,
        "--skipExistingIssues" to ImportExistsPolicy.Skip,
        help = "tells Space what should be done when issues match by external id"
    ).default(ImportExistsPolicy.Skip)

    val statusMissingPolicy by parser.mapping(
        "--replaceMissingStatus" to ImportMissingPolicy.ReplaceWithDefault,
        "--skipMissingStatus" to ImportMissingPolicy.Skip,
        help = "tells Space what should be done when it does not have a status"
    ).default(ImportMissingPolicy.Skip)

    val assigneeMissingPolicy by parser.mapping(
        "--replaceMissingAssignee" to ImportMissingPolicy.ReplaceWithDefault,
        "--skipMissingAssignee" to ImportMissingPolicy.Skip,
        help = "tells Space what should be done when it does not have a assignee"
    ).default(ImportMissingPolicy.Skip)

    // add-ons

    val assigneeMapping by parser.adding(
        "-a", "--assignee",
        help = "map assignee from external system to Space e.g. leonid.tolstoy${mappingSeparator}leo.tolstoy",
        transform = { parseMapping(this) }
    ).default(emptyList())

    val statusMapping by parser.adding(
        "-s", "--status",
        help = "map status from external system to Space e.g. in-progress${mappingSeparator}In Progress",
        transform = { parseMapping(this) }
    ).default(emptyList())

    private fun parseMapping(arg: String, separator: String = mappingSeparator): Pair<String, String> {
        val mapping = arg.split(separator)
        if (mapping.size != 2)
            throw SystemExitException("mapping format is wrong for argument: $arg", 2)

        return mapping.first() to mapping.last()
    }
}
