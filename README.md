# Import Issues into Space 🚀 
![](https://jb.gg/badges/incubator-flat-square.svg)

This script imports basic issue data (summary, description, assignee, status) from an external system into JetBrains Space.

Here you can find a list of supported import sources and sample code.

## From YouTrack
```
$ docker run public.registry.jetbrains.space/p/space/containers/space-issues-import:latest 
         --youtrackServer https://<domain>.myjetbrains.com/youtrack
         --youtrackQuery "project:SPACE module: UI #Unresolved" 
         --spaceServer http://<domain>.jetbrains.space 
         --spaceToken SECRET 
         --spaceProject key::ABC 
         -a "Leo Tolstoy::leonid.tolstoy" 
         -s "in design::In-Design" 
         -s "open::In-Design" 
         --updateExistingIssues 
         --replaceMissingStatus 
         --replaceMissingAssignee
```

## From GitHub
`In progress`

## From Jira

`In progress`

# Arguments

```
usage: [-h] [--jiraServer JIRASERVER] [--jiraQuery JIRAQUERY]
       [--jiraUser JIRAUSER] [--jiraPassword JIRAPASSWORD]
       [--youtrackServer YOUTRACKSERVER] [--youtrackQuery YOUTRACKQUERY]
       [--youtrackToken YOUTRACKTOKEN] --spaceServer SPACESERVER
       --spaceToken SPACETOKEN --spaceProject SPACEPROJECT
       [--importSource IMPORTSOURCE] [--dryRun] [--updateExistingIssues]
       [--replaceMissingStatus] [--replaceMissingAssignee] [-a ASSIGNEE]...
       [-s STATUS]... [--batchSize BATCHSIZE]

required arguments:
  --spaceServer SPACESERVER         The URL of the Space instance that you
                                    want to import into.

  --spaceToken SPACETOKEN           A personal token for a Space account that
                                    has the Import Issues permission.

  --spaceProject SPACEPROJECT       The key or ID of a project in Space into
                                    which you want to import issues. For
                                    example, key::ABC or id::42.


optional arguments:
  -h, --help                        show this help message and exit

  --jiraServer JIRASERVER           The URL of the Jira server that you want
                                    to import issues from

  --jiraQuery JIRAQUERY             A JQL query that selects the Jira issues
                                    you want to import

  --jiraUser JIRAUSER               An optional user name to use to login to
                                    Jira

  --jiraPassword JIRAPASSWORD       An optional password to use to login to
                                    Jira

  --youtrackServer YOUTRACKSERVER   The URL of the YouTrack server that you
                                    want to import issues from. Must match the one from YT Domain Settings.
                                    Typically, should end with /youtrack:
                                    https://<domain>.myjetbrains.com/youtrack

  --youtrackQuery YOUTRACKQUERY     A query that selects the YouTrack issues
                                    that you want to import.

  --youtrackToken YOUTRACKTOKEN     An optional permanent token that grants
                                    access to the YouTrack server for a
                                    specific user account. If not specified,
                                    issue data is retrieved according to the
                                    access rights that are available to the
                                    guest account.

  --importSource IMPORTSOURCE       The name of the import source. Default:
                                    External.

  --dryRun                          Runs the import script without actually
                                    creating issues.

  --updateExistingIssues,           Tells Space how to process issues when
  --skipExistingIssues              their external IDs matches previously
                                    imported issues in Space. Default:
                                    skipExistingIssues.

  --replaceMissingStatus,           Tells Space how to handle issues when the
  --skipMissingStatus               value for the status field does not exist.
                                    `replaceMissingStatus` sets it to the
                                    first unresolved status. Default:
                                    `skipMissingStatus`.

  --replaceMissingAssignee,         Tells Space how to handle issues when the
  --skipMissingAssignee             value for the assignee field does not
                                    exist. `replaceMissingAssignee` sets it to
                                    `unassigned`. Default:
                                    `skipMissingAssignee`.

  -a ASSIGNEE, --assignee ASSIGNEE  Maps the assignee in the external system
                                    to a member profile in Space. For example,
                                    leonid.tolstoy::leo.tolstoy.

  -s STATUS, --status STATUS        Maps an issue status in the external
                                    system to an issue status in Space. For
                                    example, in-progress::In Progress.

  --batchSize BATCHSIZE             The size of a batch with issues being sent
                                    to Space per request. Default: 50.


```
# Build and Run Locally

## With Docker
```
docker build . -t space-import-issues
docker run space-import-issues --youtrackServer "https://youtrack.jetbrains.com" --youtrackQuery "project:SPACE module: UI #Unresolved" --youtrackToken "perm:token" --spaceServer "https://jetbrains.team" --spaceToken "secret" --spaceProject "key::ABC"
```

## Without Docker
```
./gradlew jar  # prepare fat jar
java -jar build/libs/spaceTrackerExtIntegration-1.0-SNAPSHOT.jar  # run from jar with arguments
```

# Contributors

Pull requests are welcome! 🙌
