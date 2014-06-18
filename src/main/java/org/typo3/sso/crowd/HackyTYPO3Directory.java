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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;

// https://developer.atlassian.com/display/CROWDDEV/Creating%20a%20Custom%20Directory%20Connector
// https://answers.atlassian.com/questions/116383/how-do-you-create-a-custom-directory-connector-in-crowd
// https://answers.atlassian.com/questions/150111/need-help-setting-up-to-develop-a-custom-directory-connector-for-crowd
// https://answers.atlassian.com/questions/19501/crowd-integration-api-for-implementing-a-custom-directory-connector
// http://koblik.blogspot.nl/2008/11/atlassian-crowd-custom-directory.html

public class HackyTYPO3Directory extends InternalDirectory {
    public HackyTYPO3Directory(InternalDirectoryUtils internalDirectoryUtils, PasswordEncoderFactory passwordEncoderFactory, DirectoryDao directoryDao, UserDao userDao, GroupDao groupDao, MembershipDao membershipDao) {
        super(internalDirectoryUtils, passwordEncoderFactory, directoryDao, userDao, groupDao, membershipDao);
    }

    @Override
    public User authenticate(String usernameFromClient, PasswordCredential credentials)
            throws InactiveAccountException, InvalidAuthenticationException,
            ExpiredCredentialException, UserNotFoundException {
        //Delegate to shell script here.
        try {
						String[] cmd = {"/opt/typo3/bin/authenticate.sh", usernameFromClient, credentials.getCredential()};
            Process process = Runtime.getRuntime().exec(cmd);
            int returnValue = process.waitFor();

            switch (returnValue) {
                case 0: //success
                    User user;
                    try {
                        user = findUserByName(usernameFromClient);
                    } catch (UserNotFoundException e) {
                        //add the user to Crowd if it's not there, and populate with default groups.
                        //(groups will be managed in Crowd, then pushed to Jira and Gerrit)

                        String userInfo = waitForProcessAndReturnString("/opt/typo3/bin/getUserInfo.sh " + usernameFromClient);
                        String[] userInfoParts = userInfo.split(";");
                        //break userinfo into firstName, lastName, displayName, email
                        //Also, somehow get whether or not they've signed the CLA and any other groups.
                        String username = userInfoParts[0];
                        String firstName = userInfoParts[1];
                        String lastName = userInfoParts[2];
                        String displayName = userInfoParts[3];
                        String email = userInfoParts[4];
                        //signedCla = ;
                        //groups = ;

                        UserTemplate userTemplate = new UserTemplate(username, firstName, lastName, displayName);
                        userTemplate.setEmailAddress(email);

												//This needs the directoryId
                        user = addUser(userTemplate, null); // null password in Crowd.


                        //We might need to add default groups eventually, but Crowd might be able to do that automatically, so we'll ignore it for now.
                        //get defaultGroups
                        //for (String groupname : defaultGroups) {
                        //	addUserToGroup(username, groupName);
                        //}
												//throw new UserNotFoundException("User not found");
                    }

                    return user;
                case 1: //inactive Account
                    throw new InactiveAccountException("Inactive Account");
                case 2: //wrong password (auth failed)
                    throw new InvalidAuthenticationException("Auth Failed");
                case 3: //expired password
                    throw new ExpiredCredentialException("Expired Credentials");
                case 4: //User not found
                    throw new UserNotFoundException("User not found");
                case 5: //server error
                default:
                    throw new RemoteException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvalidCredentialException e1) {
            e1.printStackTrace();
        } catch (InvalidUserException e1) {
            e1.printStackTrace();
        } catch (UserAlreadyExistsException e1) {
            e1.printStackTrace();
        } catch (OperationFailedException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    private String waitForProcessAndReturnString(String processName) throws IOException, InterruptedException {
        String output = "";
        String line;
        Process process = Runtime.getRuntime().exec(processName);
        BufferedReader bri = new BufferedReader
                (new InputStreamReader(process.getInputStream()));
        while ((line = bri.readLine()) != null) {
            output += line;
        }
        bri.close();
        process.waitFor();

        return output;
    }

}
