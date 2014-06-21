# TYPO3 SSO Atlassian Crowd Authentication Plugin

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
