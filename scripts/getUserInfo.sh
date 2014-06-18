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
#####################################################

echo "testUsername;firstName;lastName;displayName;email@example.com"
exit 0
