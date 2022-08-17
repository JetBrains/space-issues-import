# Import Issues into Space 🚀 
![](https://jb.gg/badges/incubator-flat-square.svg)

This script imports basic issue data (summary, description, assignee, status) from an external system into JetBrains Space.

Here you can find a list of supported import sources and sample code.

## Execution examples

### From YouTrack
```
$ docker run public.registry.jetbrains.space/p/space/containers/space-issues-import:latest 
        --importSource YouTrack
        --youtrackServer https://<domain>.myjetbrains.com/youtrack
        --youtrackQuery "project:SPACE module: UI #Unresolved" 
        --spaceServer https://<domain>.jetbrains.space 
        --spaceToken SECRET 
        --spaceProject key::ABC 
        -a "Leo Tolstoy::leonid.tolstoy" 
        -s "in design::In-Design" 
        -s "open::In-Design" 
        --updateExistingIssues 
        --replaceMissingStatus 
        --replaceMissingAssignee
```

### From Jira

```
$ docker run public.registry.jetbrains.space/p/space/containers/space-issues-import:latest
        --importSource Jira
        --spaceServer https://<domain>.jetbrains.space 
        --spaceToken SECRET 
        --spaceProject key::ABC
        --jiraServer https://<domain>.atlassian.net.
        --jiraUser USER 
        --jiraApiToken SECRET
        -a "Leo Tolstoy::leonid.tolstoy" 
        -s "in design::In-Design" 
        -s "open::In-Design" 
        --updateExistingIssues 
        --replaceMissingStatus 
        --replaceMissingAssignee
```

### From Notion

```
$ docker run public.registry.jetbrains.space/p/space/containers/space-issues-import:latest 
        --importSource Notion 
        --notionQuery "{\"filter\":{},\"sorts\":{},\"start_cursor\":\"\",\"page_size\":100}" 
        --notionToken SECRET 
        --notionDatabaseId SECRET 
        --notionAssigneeProperty name::Assignee 
        --notionAssigneePropertyMappingType name 
        --notionStatusProperty name::Status 
        --notionStatusPropertyMappingType name 
        --notionTagProperty name::Project 
        --notionTagPropertyMappingType name 
        --tagPropertyMappingType name 
        --spaceServer "https://<domain>.jetbrains.space" 
        --spaceToken SECRET 
        --spaceProject key::ABC 
        --spaceBoard name::Tasks 
        --replaceMissingStatus 
        --replaceMissingAssignee 
        --assignee "John Doe::john.doe" 
        --status "TODO::🔍 TODO" 
        --status "Done::✅ Done" 
        --tag "📱 Android::🤖 Android" 
        --tag "🍏 iOS::🍏 iOS"
```

## Arguments

```
usage: [-h] [--jiraServer JIRASERVER] [--jiraQuery JIRAQUERY] [--jiraUser JIRAUSER] [--jiraApiToken JIRAAPITOKEN]
       [--youtrackServer YOUTRACKSERVER] [--youtrackQuery YOUTRACKQUERY] [--youtrackToken YOUTRACKTOKEN]
       [--notionDatabaseId NOTIONDATABASEID] [--notionToken NOTIONTOKEN] [--notionAssigneeProperty NOTIONASSIGNEEPROPERTY]
       [--notionStatusProperty NOTIONSTATUSPROPERTY] [--notionTagProperty NOTIONTAGPROPERTY]
       [--notionAssigneePropertyMappingType NOTIONASSIGNEEPROPERTYMAPPINGTYPE]
       [--notionStatusPropertyMappingType NOTIONSTATUSPROPERTYMAPPINGTYPE] [--notionTagPropertyMappingType NOTIONTAGPROPERTYMAPPINGTYPE]
       [--tagPropertyMappingType TAGPROPERTYMAPPINGTYPE] [--notionQuery NOTIONQUERY] --spaceServer SPACESERVER --spaceToken SPACETOKEN
       --spaceProject SPACEPROJECT [--spaceBoard SPACEBOARD] [--importSource IMPORTSOURCE] [--dryRun] [--updateExistingIssues]
       [--replaceMissingStatus] [--replaceMissingAssignee] [-a ASSIGNEE]... [-s STATUS]... [-t TAG]... [--batchSize BATCHSIZE] [--debug]

required arguments:
  --spaceServer SPACESERVER                                               The URL of the Space instance that you want to import into.

  --spaceToken SPACETOKEN                                                 A personal token for a Space account that has the Import Issues
                                                                          permission.

  --spaceProject SPACEPROJECT                                             The key or ID of a project in Space into which you want to
                                                                          import issues. For example, key::ABC or id::42.


optional arguments:
  -h, --help                                                              show this help message and exit

  --jiraServer JIRASERVER                                                 The URL of the Jira server that you want to import issues from

  --jiraQuery JIRAQUERY                                                   An optional JQL query that selects the Jira issues you want to
                                                                          import

  --jiraUser JIRAUSER                                                     An optional user name to use to login to Jira

  --jiraApiToken JIRAAPITOKEN                                             An optional API token to use to login to Jira

  --youtrackServer YOUTRACKSERVER                                         The URL of the YouTrack server that you want to import issues
                                                                          from.

  --youtrackQuery YOUTRACKQUERY                                           A query that selects the YouTrack issues that you want to
                                                                          import.

  --youtrackToken YOUTRACKTOKEN                                           An optional permanent token that grants access to the YouTrack
                                                                          server for a specific user account. If not specified, issue data
                                                                          is retrieved according to the access rights that are available
                                                                          to the guest account.

  --notionDatabaseId NOTIONDATABASEID                                     The ID of the Notion database that you want to import issues
                                                                          from.

  --notionToken NOTIONTOKEN                                               A token to access the Notion API.

  --notionAssigneeProperty NOTIONASSIGNEEPROPERTY                         The name or ID of a property in Notion which will be mapped to
                                                                          the Space issue assignee. For example, name::Title or
                                                                          id::uuid-uuid-uuid

  --notionStatusProperty NOTIONSTATUSPROPERTY                             The name or ID of a property in Notion which will be mapped to
                                                                          the Space issue status. For example, name::Title or
                                                                          id::uuid-uuid-uuid

  --notionTagProperty NOTIONTAGPROPERTY                                   The name or ID of a property in Notion which will be mapped to
                                                                          the Space issue tag. For example, name::Title or
                                                                          id::uuid-uuid-uuid

  --notionAssigneePropertyMappingType NOTIONASSIGNEEPROPERTYMAPPINGTYPE   id, name, or email. Default: name. For --assignee command, what
                                                                          to map on the Notion side, e.g. '--assignee uuid-uuid::john.doe'
                                                                          for 'id' vs '--assignee John Doe::john.doe' for 'name'. This
                                                                          argument will be used in case the property corresponds to a
                                                                          person, select or multiselect. Plain value (name) will be used
                                                                          otherwise (for email, phone number, text, title, etc.). Note
                                                                          that email will only be used in case the property corresponds to
                                                                          a person (including created & edited by).

  --notionStatusPropertyMappingType NOTIONSTATUSPROPERTYMAPPINGTYPE       id or name. Default: name. For --status command, what to map on
                                                                          the Notion side, e.g. '--tag uuid-uuid::To Do' for 'id' vs
                                                                          '--tag To Do::To Do' for 'name'.

  --notionTagPropertyMappingType NOTIONTAGPROPERTYMAPPINGTYPE             id or name. Default: name. For --tag command, what to map on the
                                                                          Notion side, e.g. '--tag uuid-uuid::Android' for 'id' vs '--tag
                                                                          Android::Android' for 'name'.

  --tagPropertyMappingType TAGPROPERTYMAPPINGTYPE                         [supported only for Notion] id or name. Default: name. Please
                                                                          add 'View project data' permission to your Space integration if
                                                                          you use 'name'. For --tag command, what to map on the Space
                                                                          side, e.g. '--tag Android::space-tag-id' for 'id' vs '--tag
                                                                          Android::Android' for 'name'.

  --notionQuery NOTIONQUERY                                               JSON string which will be used as the request body to
                                                                          databases/:id/query. By default, all the cards from the board
                                                                          are exported.

  --spaceBoard SPACEBOARD                                                 [supported only for Notion] The name or ID of a board in Space
                                                                          into which you want to import issues. For example, name::Tasks
                                                                          or id:DRrHX45Jsxl

  --importSource IMPORTSOURCE                                             The name of the import source. Default: External.

  --dryRun                                                                Runs the import script without actually creating issues.

  --updateExistingIssues, --skipExistingIssues                            Tells Space how to process issues when their external IDs
                                                                          matches previously imported issues in Space. Default:
                                                                          skipExistingIssues.

  --replaceMissingStatus, --skipMissingStatus                             Tells Space how to handle issues when the value for the status
                                                                          field does not exist. `replaceMissingStatus` sets it to the
                                                                          first unresolved status. Default: `skipMissingStatus`.

  --replaceMissingAssignee, --skipMissingAssignee                         Tells Space how to handle issues when the value for the assignee
                                                                          field does not exist. `replaceMissingAssignee` sets it to
                                                                          `unassigned`. Default: `skipMissingAssignee`.

  -a ASSIGNEE, --assignee ASSIGNEE                                        Maps the assignee in the external system to a member profile in
                                                                          Space. For example, leonid.tolstoy::leo.tolstoy.

  -s STATUS, --status STATUS                                              Maps an issue status in the external system to an issue status
                                                                          in Space. For example, in-progress::In Progress.

  -t TAG, --tag TAG                                                       [supported only for Notion] Maps the tag in the external system
                                                                          to a tag in Space. For example, external-tag::space-tag-id.
                                                                          Please remember to specify the tag property for the external
                                                                          system.

  --batchSize BATCHSIZE                                                   The size of a batch with issues being sent to Space per request.
                                                                          Default: 50.

  --debug                                                                 Runs the import script in debug mode.
```
## Build and Run Locally

### With Docker
```
docker build . -t space-import-issues
docker run space-import-issues --youtrackServer "https://youtrack.jetbrains.com" --youtrackQuery "project:SPACE module: UI #Unresolved" --youtrackToken "perm:token" --spaceServer "https://jetbrains.team" --spaceToken "secret" --spaceProject "key::ABC"
```

### Without Docker
```
./gradlew jar  # prepare fat jar
java -jar build/libs/space-issues-import-1.0.jar  # run from jar with arguments
```

## Contributors

Pull requests are welcome! 🙌
