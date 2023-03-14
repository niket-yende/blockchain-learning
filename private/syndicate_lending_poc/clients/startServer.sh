#!/bin/bash

rpc_ports=(10006 10009 10012 10015)
count=0
while [ $count -lt 4 ]
do
    for rpc_port in "${rpc_ports[@]}"; do
        echo "Current rpc_port : $rpc_port";
        line_count=`sudo lsof -ti :${rpc_port} | wc -l`
        echo "line count : $line_count"
        if  [ $line_count -gt 0 ];
        then
            echo "Increase the counter"
            count=$((count + 1))
        fi
        echo $count
    done
done

if [ $count -eq 4 ];
then
	echo "All nodes are up, start the spring boot server"
	../gradlew runCommonServer
else
	echo "Some of the nodes failed to come up"
fi
