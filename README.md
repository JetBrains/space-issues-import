# Import Issues into Space 🚀 
![](https://jb.gg/badges/incubator-flat-square.svg)

This script imports basic issues information (summary, description, assignee, status) from external systems into JetBrains Space, see the list and examples below.

Full documentation is here.

## YouTrack
```
$ docker run TODO/IMAGE/space-import-issues 
         --youtrackServer https://youtrack.jetbrains.com 
         --youtrackQuery "project:SPACE module: UI #Unresolved" 
         --spaceServer http://jetbrains.team 
         --spaceToken SECRET 
         --spaceProject key::ABC 
         -a "Leo Tolstoy::leonid.tolstoy" 
         -s "in design::In-Design" 
         -s "open::In-Design" 
         --updateExistingIssues 
         --replaceMissingStatus 
         --replaceMissingAssignee
```

## GitHub
`In progress`

# Arguments
```
usage: [-h] --youtrackServer YOUTRACKSERVER --youtrackQuery YOUTRACKQUERY
       [--youtrackToken YOUTRACKTOKEN] --spaceServer SPACESERVER
       --spaceToken SPACETOKEN --spaceProject SPACEPROJECT
       [--importSource IMPORTSOURCE] [--dryRun] [--updateExistingIssues]
       [--replaceMissingStatus] [--replaceMissingAssignee] [-a ASSIGNEE]...
       [-s STATUS]...

required arguments:
  --youtrackServer YOUTRACKSERVER   URL of a YouTrack server to import issues
                                    from

  --youtrackQuery YOUTRACKQUERY     a query to select issues from YouTrack
                                    server

  --spaceServer SPACESERVER         URL of a Space server to operate on

  --spaceToken SPACETOKEN           Personal Token with Import Issues
                                    permission of a Space server

  --spaceProject SPACEPROJECT       The key or ID of a project in Space to
                                    import issues into. E.g. key::ABC or
                                    id::42


optional arguments:
  -h, --help                        show this help message and exit

  --youtrackToken YOUTRACKTOKEN     YouTrack token to access server;
                                    will fetch issues as a guest otherwise

  --importSource IMPORTSOURCE       Import source name

  --dryRun                          tell Space to run import without actually
                                    creating issues

  --updateExistingIssues,           tell Space what should be done when
  --skipExistingIssues              issues match by external id

  --replaceMissingStatus,           tell Space what should be done when it
  --skipMissingStatus               does not have a status

  --replaceMissingAssignee,         tell Space what should be done when it
  --skipMissingAssignee             does not have a assignee

  -a ASSIGNEE, --assignee ASSIGNEE  map assignee from external system to Space
                                    e.g. leonid.tolstoy::leo.tolstoy

  -s STATUS, --status STATUS        map status from external system to Space
                                    e.g. in-progress::In Progress
```

# Build and run locally

## With Docker
```
docker build . -t space-import-issues
docker run space-import-issues --youtrackServer "https://youtrack.jetbrains.com" --youtrackQuery "project:SPACE module: UI #Unresolved" --youtrackToken "perm:token" --spaceServer "https://jetbrains.team" --spaceToken "sp:token" --spaceProjectId "key:MPR2"
```

## Without Docker
```
./gradlew jar  # prepare fat jar
java -jar build/libs/spaceTrackerExtIntegration-1.0-SNAPSHOT.jar  # run from jar with arguments
```

# Contributors

Pull requests are welcome! 🙌
