#!/bin/bash
echo "Since this script starts our actual server, it first calls the compile.sh script to make sure it is compiled."
./compile.sh
echo "This script starts our actual server files, with a predetermined port that is also known by the client testing files and scripts.  It does not itself go through all functions of the server, but rather sets the server up to be connected to by the client testing programs, started and run via the client testing scripts, and demonstrates the functionality of the server that way.  It starts the server on port 2356 and stores the gossip and peer data files in a directory script_testing_data so that they are separate from the actual servers data files."
echo "Since this actually starts our server, it will run in an infinite loop as the server should do, waiting for clients to connect.  As a result, when you are done testing / running the client test scripts, you will have to manually close the server program to stop it, such as with an abort command like Ctrl-C"

java netprog/Main -d ./script_testing_data -p 2356 -v

