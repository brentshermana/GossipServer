#!/bin/bash/
#pass along all arguments to the program
javac netprog/client/clientapp/ClientAppForTCPTestScript.java
java netprog/client/clientapp/ClientAppForTCPTestScript -T -s 127.0.0.1 -p 2356 -m "TCP Gossip test message from command line" -t "2017-02-09-16-18-20-001Z"

