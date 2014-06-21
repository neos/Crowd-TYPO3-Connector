# TYPO3 SSO Atlassian Crowd Authentication Plugin

This was developed to work with Crowd 2.7.2.

In order to compile, run the following command:

`
./gradlew clean jar
`

Then, you need to copy the following two files into Crowd:

```
cp build/libs/typo3-auth.jar /[path-to-crowd]/crowd-webapp/WEB-INF/lib/
cp json-simple-1.1.1.jar /[path-to-crowd]/crowd-webapp/WEB-INF/lib/
```

After that, restart crowd.

* Then, the Directory Class is: org.typo3.sso.crowd.TYPO3SsoDirectory
* Set the API key in an attribute called "apiKey"
* Set the Default User Groups in an attribute called "defaultGroups" (which is a comma-separated group list WITH NO WHITESPACE)
* If the user has a signed CLA, he additionally gets the User Group from "contributorGroup"




### Development

* Development Script for Sebastian:

```
ps aux | grep crowd
kill [crowdId]

./gradlew clean jar
cp build/libs/typo3-jira-auth.jar /Users/sebastian/Downloads/atlassian-crowd-2.7.2/crowd-webapp/WEB-INF/lib/
/Users/sebastian/Downloads/atlassian-crowd-2.7.2/start_crowd.sh
```