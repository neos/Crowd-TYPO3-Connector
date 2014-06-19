#!/bin/sh

#####################################################
#                                                   #
# Arguments:                                        #
#    $1: username                                   #
#                                                   #
# Output to StdOut:                                 #
#    username;firstName;lastName;displayName;email  #
#                                                   #
# Things that would be nice to include later:       #
#    signedCLAboolean                               #
#    groups                                         #
#                                                   #
# Exit Status Codes:                                #
#    0: Success (retrieved and echoed the userdata) #
#    1: General Error                               # 
#    4: User not found                              #
#    5: server error                                #
#                                                   #
# User fields stored on typo3.org:                  #
#    username                                       #
#    password                                       #
#    user-name:      Name                           #
#    user-email:     E-mail                         #
#    user-www:       Homepage                       #
#    user-title:     Title                          #
#    user-company:   Company                        #
#    user-address:   Address                        #
#    user-zip:       Zipcode                        #
#    user-city:      City                           #
#    user-country:   Country                        #
#    user-telephone: Telephone                      #
#    user-fax:       Fax                            #
#    tx_t3ouserimage_pi1[image]                     #
#                                                   #
#####################################################

echo "testUsername;firstName;lastName;displayName;email@example.com"
exit 0
