#!/bin/bash

#变量传参
EXEC=$1
DIR=$2
DATA=$3
#执行命令
cd $DIR
python3 ./$EXEC $DATA
