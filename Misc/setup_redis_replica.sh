#!/bin/bash
# Simplest way to execute this script:
# curl --remote-name-all http://10.168.130.49:8080/fileserver/devcloud_redis/setup_redis_replica.sh
# execute below script by executing this command - sh ./setup_redis_replica.sh
#

#  Set the root user

if [ "${USER}" != "root" ]; then
        echo "You must login as 'root' user to execute this script."
        echo "execute: sudo su - root"
		echo "execute: cd /home/dc-user"
        exit
fi

export HOME_DIR="/home/dc-user"
curl --remote-name-all http://10.168.130.49:8080/fileserver/find_replace.sh

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 1 - Installing init scripting"
echo "------------------------------------------------------------------------------------"

FILE_NAME="/etc/redis/6379.conf"

QUES="Do you wish to setup Redis Replica (aka Slave) (y/n)? "
read -r -p "$QUES" response
response=${response,,} # tolower

# If yes or y
if [[ $response =~ ^(yes|y| ) ]] || [[ -z $response ]]; then
	QUES="Enter IP address of Redis Master Server: "
	read -r -p "$QUES" redisMasterHost
	QUES="Enter Port of Redis Master Server (press enter to keep default 6379): "
	read -r -p "$QUES" redisMasterPort
	if [ -z "$redisMasterPort" ]
	then
		redisMasterPort="6379"	
	fi
	
	FIND_STRING="# replicaof <masterip> <masterport>"
	REPLACE_STRING="replicaof ${redisMasterHost} ${redisMasterPort}"
	source ./find_replace.sh $FILE_NAME "$FIND_STRING" "$REPLACE_STRING"
	
	QUES="Enter Redis Master Server password if its password protected (press enter to skip): "
	read -r -p "$QUES" redisPassword
	if [ ! -z "$redisPassword" ]
	then
		FIND_STRING="# masterauth <master-password>"
		REPLACE_STRING="masterauth ${redisPassword}"
		source ./find_replace.sh $FILE_NAME "$FIND_STRING" "$REPLACE_STRING"	
	fi
else
	exit
fi

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 2 - Restarting the redis service"
echo "------------------------------------------------------------------------------------"

source service redis_6379 restart

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 3 - Checking if the redis service is working"
echo "------------------------------------------------------------------------------------"
res="$(printf 'info replication\n' | /usr/local/bin/redis-cli)"
echo "/usr/local/bin/redis-cli"
echo "127.0.0.1:6379>info"
echo "${res}"
echo ""
echo "Run the following command, to promote redis SLAVE as MASTER"
echo ""
echo ">SLAVEOF NO ONE"
echo ""

echo "------------------------------------------------------------------------------------"
echo "Step 4 - Lets checking Redis Server replication information"
echo "------------------------------------------------------------------------------------"
res="$(printf 'info replication\n' | /usr/local/bin/redis-cli)"
echo "/usr/local/bin/redis-cli -h ${redisMasterHost} -p ${redisMasterPort}"
echo "127.0.0.1:6379>info"
echo "${res}"
echo ""


