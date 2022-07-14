#!/bin/bash

FILE_NAME="$1"
FIND_STRING="$2"
REPLACE_STRING="$3"

#echo "grep -n "${FIND_STRING}" $FILE_NAME | cut -d: -f 1 | tail -n 1"

LINE_NUMBER=`grep -n "${FIND_STRING}" $FILE_NAME | cut -d: -f 1 | tail -n 1`
FIRST_LINE_NUMBER=$LINE_NUMBER

while [ ! -z "$LINE_NUMBER" ]
do
	#echo "LINE_NUMBER=$LINE_NUMBER"
	sed -i "${LINE_NUMBER} d" $FILE_NAME
	LINE_NUMBER=`grep -n "${FIND_STRING}" $FILE_NAME | cut -d: -f 1 | tail -n 1`
done

if [ ! -z "$FIRST_LINE_NUMBER" ]
then
	#echo "FIRST_LINE_NUMBER=$FIRST_LINE_NUMBER"
	sed -i "${FIRST_LINE_NUMBER} i ${REPLACE_STRING}" $FILE_NAME
fi

echo "$FILE_NAME file: Replaced $FIND_STRING line with $REPLACE_STRING"
