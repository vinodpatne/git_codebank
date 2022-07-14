#!/bin/bash
# Simplest way to execute this script:
# curl --remote-name-all http://10.168.130.49:8080/fileserver/devcloud_redis/setup_redis.sh
# execute below script by executing this command - sh ./setup_redis.sh
#

#  Set the root user

if [ "${USER}" != "root" ]; then
        echo "You must login as 'root' user to execute this script."
        echo "execute: sudo su - root"
		echo "execute: cd /home/dc-user"
        exit
fi

export HOME_DIR="/home/dc-user"

export BOOT_FILE="/etc/rc.d/rc.local"

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 1 - Install required software packages: wget, gcc, make"
echo "------------------------------------------------------------------------------------"
echo .

REQ_EXEC=/usr/bin/wget
if [ ! -f "$REQ_EXEC" ]; then
  yum install wget
fi

REQ_EXEC=/usr/bin/gcc
if [ ! -f "$REQ_EXEC" ]; then
  yum install gcc
fi

REQ_EXEC=/usr/bin/make
if [ ! -f "$REQ_EXEC" ]; then
  yum install make
fi

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 2 - Downloading Redis"
echo "------------------------------------------------------------------------------------"

cd $HOME_DIR
if [ ! -f redis-5.0.6.tar.gz ]; then
    echo "Download file from file server..."
	curl --remote-name-all http://10.168.130.49:8080/fileserver/devcloud_redis/redis-5.0.6.tar.gz
fi
curl --remote-name-all http://10.168.130.49:8080/fileserver/find_replace.sh
tar -xf redis-5.0.6.tar.gz

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 3 - Compiling & installing redis"
echo "------------------------------------------------------------------------------------"

cd redis-5.0.6
cd deps
echo "compile the packages"
make hiredis lua jemalloc linenoise
make geohash-int
cd ../
echo "run ‘make’ & ‘make install’"

make

echo "------------------------------------------------------------------------------------"

make install

echo "Installation Completed"

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 4 - Installing init scripting"
echo "------------------------------------------------------------------------------------"

cd utils

QUES="Do you wish to setup Redis Server with default configuration (y/n)? "
read -r -p "$QUES" response
response=${response,,} # tolower

alias exit=return
# If yes or y
if [[ $response =~ ^(yes|y| ) ]] || [[ -z $response ]]; then
	echo "we made install_server.sh as non-interactive by redirecting input from echo command."
	echo " without pumping any value into the script, the default values are used."
	echo -n | source ./install_server.sh

else
	echo "Running interactive installation..."
	source ./install_server.sh
	#echo -e "${PORT}\n${CONFIG_FILE}\n${LOG_FILE}\n${DATA_DIR}\n${EXECUTABLE}\n" | sudo utils/install_server.sh
fi
unalias exit

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 5 - Making redis server accessible from remote system"
echo "------------------------------------------------------------------------------------"

cd $HOME_DIR

#look for ‘bind 127.0.0.1’. We can either replace 127.0.0.1 with 0.0.0.0 or add IP address of our server to it.
FIND_STRING="bind 127.0.0.1"
REPLACE_STRING="bind 0.0.0.0"

FILE_NAME="/etc/redis/6379.conf"
source ./find_replace.sh $FILE_NAME "$FIND_STRING" "$REPLACE_STRING"

chown -R dc-user:dc-user .
chmod -R 777 /usr/local/bin

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 6 - Restarting the redis service"
echo "------------------------------------------------------------------------------------"

source service redis_6379 restart

echo ""
echo "------------------------------------------------------------------------------------"
echo "Step 7 - Checking if the redis service is working"
echo "------------------------------------------------------------------------------------"
res="$(printf 'ping\n' | /usr/local/bin/redis-cli)"
echo "/usr/local/bin/redis-cli"
echo "127.0.0.1:6379>ping"
echo "${res}"
echo ""
if [ "$res" == "PONG" ]
then
   echo "Received 'PONG' as response from Redis server."
   echo "It indicates that Redis is installed and started successfully."
fi
echo ""
echo ""

#Reference: https://linuxtechlab.com/how-install-redis-server-linux/
