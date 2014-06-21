package org.typo3.sso.crowd;

import com.atlassian.crowd.directory.InternalDirectory;
import com.atlassian.crowd.directory.InternalDirectoryUtils;
import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.embedded.spi.DirectoryDao;
import com.atlassian.crowd.embedded.spi.GroupDao;
import com.atlassian.crowd.embedded.spi.MembershipDao;
import com.atlassian.crowd.embedded.spi.UserDao;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.model.user.InternalUser;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Connector to TYPO3 SSO
 */
public class TYPO3SsoDirectory extends InternalDirectory {

	private static final Logger logger = LoggerFactory.getLogger(InternalDirectory.class);// configuration parameters
	private HashMap<String, String> attributes = new HashMap<String, String>();


	public TYPO3SsoDirectory(InternalDirectoryUtils internalDirectoryUtils, PasswordEncoderFactory passwordEncoderFactory, DirectoryDao directoryDao, UserDao userDao, GroupDao groupDao, MembershipDao membershipDao) {
		super(internalDirectoryUtils, passwordEncoderFactory, directoryDao, userDao, groupDao, membershipDao);
	}

	@Override
	public void setAttributes(Map<String, String> attrs) {
		for (Map.Entry<String, String> entry : attrs.entrySet()) {
			attributes.put(entry.getKey(), entry.getValue());
		}
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
			method.addParameter("apiKey", attributes.get("apiKey"));
			method.addParameter("username", username);
			method.addParameter("returnUserInfo", "1");
			method.addParameter("password", credentials.getCredential());

			String responseString = "";

			try {
				int returnCode = client.executeMethod(method);
				logger.info("Authentication request returned with code: " + returnCode);

				br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
				String readLine;
				while (((readLine = br.readLine()) != null)) {
					logger.info(readLine);
					responseString = responseString.concat(readLine);
				}
			} catch (Exception e) {
				System.err.println(e);
			} finally {
				method.releaseConnection();
				if (br != null) try {
					br.close();
				} catch (Exception fe) {
				}
			}

			if (responseString.equals("0")) {
				// Authentication not successful
				throw new InvalidAuthenticationException("Auth Failed");
			} else {
				User user;

				JSONObject parsedResponseString = (JSONObject) JSONValue.parse(responseString);
				logger.info("PARSED RESPONSE: " + parsedResponseString);

				try {
					user = findUserByName(username);
					logger.info("Found user: " + user);

					if (parsedResponseString.get("tx_t3ocla_hassignedcla").equals("1")) {
						try {
							addUserToGroup(username, attributes.get("contributorGroup"));
						} catch (MembershipAlreadyExistsException e1) {
							// silently swallow this exception
						}
					}

					return user;
				} catch (UserNotFoundException e) {
					//add the user to Crowd if it's not there, and populate with default groups.
					//(groups will be managed in Crowd, then pushed to Jira)

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



					//We might need to add default groups eventually, but Crowd might be able to do that automatically, so we'll ignore it for now.
					//get defaultGroups

					String[] defaultGroups = attributes.get("defaultGroups").split(",");

					try {
						for (String group : defaultGroups) {
							addUserToGroup(username, group);
						}

						if (parsedResponseString.get("tx_t3ocla_hassignedcla").equals("1")) {
							addUserToGroup(username, attributes.get("contributorGroup"));
						}
					} catch (MembershipAlreadyExistsException e1) {
						// silently swallow this exception
					}

					//throw new UserNotFoundException("User not found");
					return user;
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
		} catch (GroupNotFoundException e1) {
			e1.printStackTrace();
		}

		return null;
	}
}
