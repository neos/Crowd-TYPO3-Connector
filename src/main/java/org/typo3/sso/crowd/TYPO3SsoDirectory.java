package org.typo3.sso.crowd;

import com.atlassian.crowd.directory.InternalDirectory;
import com.atlassian.crowd.directory.InternalDirectoryUtils;
import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.embedded.spi.DirectoryDao;
import com.atlassian.crowd.embedded.spi.GroupDao;
import com.atlassian.crowd.embedded.spi.MembershipDao;
import com.atlassian.crowd.embedded.spi.UserDao;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.model.user.UserTemplate;
import com.atlassian.crowd.password.factory.PasswordEncoderFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Connector to TYPO3 SSO
 */
public class TYPO3SsoDirectory extends InternalDirectory {

    private static final Logger logger = LoggerFactory.getLogger(InternalDirectory.class);// configuration parameters

    public TYPO3SsoDirectory(InternalDirectoryUtils internalDirectoryUtils, PasswordEncoderFactory passwordEncoderFactory, DirectoryDao directoryDao, UserDao userDao, GroupDao groupDao, MembershipDao membershipDao) {
        super(internalDirectoryUtils, passwordEncoderFactory, directoryDao, userDao, groupDao, membershipDao);
    }

		/*
		 * Since we extend Internal Directory, and groups will be managed in this directory,
		 * not on typo3.org, we can use Crowd's nested groups support.
		 */
		public boolean supportsNestedGroups() {
				return true;
		}

		/*
		 * attributes
		 *   ATTRIBUTE_PASSWORD_COMPLEXITY_MESSAGE 
		 *   ATTRIBUTE_PASSWORD_HISTORY_COUNT
		 *   ATTRIBUTE_PASSWORD_MAX_ATTEMPTS
		 *   ATTRIBUTE_PASSWORD_MAX_CHANGE_TIME
		 *   ATTRIBUTE_PASSWORD_REGEX
		 *   ATTRIBUTE_USER_ENCRYPTION_METHOD
		 *   DESCRIPTIVE_NAME
		 *
		 * What about default groups like the Internal Directory?
		 */

    @Override
    public User authenticate(String username, PasswordCredential credentials)
            throws InactiveAccountException, InvalidAuthenticationException,
            ExpiredCredentialException, UserNotFoundException {

        try {
            HttpClient client = new HttpClient();

            BufferedReader br = null;

            PostMethod method = new PostMethod("https://typo3.org/services/authenticate.php");
            method.addParameter("apiKey", "9ac8173cabf179a1277190a71b6dd009187cabbe7e1ff78a43278919489aac28da10cff83cad8194622b3bc1983710a2");
            method.addParameter("username", username);
            method.addParameter("returnUserInfo", "1");
            method.addParameter("password", credentials.getCredential());

            String responseString = "";

            try {
                int returnCode = client.executeMethod(method);
                logger.info("Authentication request returned with code: " + returnCode);

                br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
                String readLine;
                while(((readLine = br.readLine()) != null)) {
                    logger.info(readLine);
                    responseString = responseString.concat(readLine);
                }
            } catch (Exception e) {
                System.err.println(e);
            } finally {
                method.releaseConnection();
                if(br != null) try { br.close(); } catch (Exception fe) {}
            }

            if (responseString.equals("0")) {
                // Authentication not successful
                throw new InvalidAuthenticationException("Auth Failed");
            } else {
                User user;
                try {
                    user = findUserByName(username);
                    return user;
                } catch (UserNotFoundException e) {
                    //add the user to Crowd if it's not there, and populate with default groups.
                    //(groups will be managed in Crowd, then pushed to Jira)

                    JSONObject parsedResponseString = (JSONObject) JSONValue.parse(responseString);
                    logger.info("PARSED RESPONSE: " + parsedResponseString);

                    if (!username.equals(parsedResponseString.get("username"))) {
                        throw new InvalidAuthenticationException("Something went terribly wrong");
                    }

                    UserTemplate userTemplate = new UserTemplate(username);
                    userTemplate.setFirstName((String) parsedResponseString.get("firstName"));
                    userTemplate.setLastName((String) parsedResponseString.get("lastName"));
                    userTemplate.setDisplayName((String) parsedResponseString.get("name"));
                    userTemplate.setEmailAddress((String) parsedResponseString.get("email"));
                    userTemplate.setDirectoryId(getDirectoryId());

                    user = addUser(userTemplate, null); // null password in Crowd.
                    return user;


                    //We might need to add default groups eventually, but Crowd might be able to do that automatically, so we'll ignore it for now.
                    //get defaultGroups
                    //for (String groupname : defaultGroups) {
                    //	addUserToGroup(username, groupName);
                    //}
                    //throw new UserNotFoundException("User not found");
                }
            }
        } catch (InvalidUserException e1) {
            e1.printStackTrace();
        } catch (UserAlreadyExistsException e1) {
            e1.printStackTrace();
        } catch (OperationFailedException e1) {
            e1.printStackTrace();
        } catch (InvalidCredentialException e1) {
            e1.printStackTrace();
        }

        return null;
    }
}
