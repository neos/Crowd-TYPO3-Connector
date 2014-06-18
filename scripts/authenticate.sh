#!/bin/sh

########################################
#                                      #
# Arguments:                           #
#    $1: username                      #
#    $2: password (in plain text!)     #
#                                      #
# Exit Status Codes:                   #
#    0:  success (user authenticated)  #
#    1:  inactive Account              #
#    2:  wrong password (auth failed)  #
#    3:  expired password              #
#    4:  User not found                #
#    5:  server error                  #
#                                      #
# This must not output anything to     #
# StdOut or StdError. This should only #
# output one of the exit status codes  #
# Output can cause the Java class that #
# uses this script to hang.            #
#                                      #
########################################

echo $1 >> /tmp/crowdDirectoryLog
echo $2 >> /tmp/crowdDirectoryLog

exit 0
