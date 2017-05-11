#!/bin/bash/
echo "Since this script starts our actual server, it first calls the compile.sh script to make sure it is compiled."
./compile.sh
echo "This will start a witness server in a separate terminal that is designed to run with the testing clients and server running at the same time, started via their testing scripts.  This peer witness server is set to run on port 56050, which is the port of one of the peers in one of the client testing scripts.  As a result, the actual server will forward messages to this server that it receives from the testing client.  Those messages will be stored in the data for this peer witness server, contained in the server_peer_witness_data directory."
echo "It should also be noted that since the server and client are now both started from the same program that this will also start the client in the same terminal.  The client is started as a UDP client and no commands
are given to it unless you type them in."
java netprog/Master -d ./server_peer_witness_data -p 56050 -v -U


