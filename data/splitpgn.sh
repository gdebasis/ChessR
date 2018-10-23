#!/bin/bash

if [ $# -lt 2 ]
then
	echo "usage $0 <pgn file> <num games>"
	exit
fi

pgnfile=$1
c=0
findex=0
ngames=`expr $2 \* 2`

cp /dev/null $pgnfile.$findex

while read line
do
	if [[ ${#line} == 1 ]]
	then
		c=`expr $c + 1`
		if [[ $c == $ngames ]]
		then
			findex=`expr $findex + 1`
			cp /dev/null $pgnfile.$findex
			c=0
		fi
	fi
	echo $line >> $pgnfile.$findex	
done < $pgnfile
