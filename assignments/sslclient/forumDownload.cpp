

#include "forumDownload.h"
#include "openssl\bio.h"
#include "openssl\ssl.h"
#include "openssl\err.h"
#include <openssl/crypto.h>




#include <sys/types.h>

#include <stdio.h>
#include <string.h>
#include <errno.h>



#include <sys/socket.h>
#include <netinet/n.h>
#include <arpra/inet.h>
#include <netdb.h>
#include <unistd.h>


#include <iostream>

using namespace std;



const SSL_METHOD* meth;
SSL_CTX* ctx;
SSL *ssl;
X509 *peer_cert;
char* CA_CERT = "debateDecide_cert.pem";
int sock;
struct sockaddr_in server_addr;
char* s_ipaddr = "163.118.78.40";
int s_port = 443;
int err;
char wbuf[1000] = "GET\r\n";

#define CHK_ERR(a, b) {if(a<=1) printf("%s: %d\n", b, a);}
#define CERTFILE "certs.pem"

int pem_passwd_cb(char* buf, int size, int rwflag, void *password) {
	strncpy(buf, (char*)password, size);
	buf[size - 1] = '\0';
	return strlen(buf);
}


int main() {
	
	SSL_load_error_strings();
	ERR_load_BIO_strings();

	SSL_library_init();

	meth = SSLv23_method();
	ctx = SSL_CTX_new(meth);
	//setup certificate here

	//setup personal certificate
	SSL_CTX_use_certificate_file(ctx, CERTFILE, SSL_FILETYPE_PEM);
	SSL_use_certificate_file(ssl, CERTFILE, SSL_FILETYPE_PEM);

	//setup certificate chain
	SSL_CTX_use_certificate_chain_file(ctx, CERTFILE);

	//setup password
	SSL_CTX_set_default_passwd_cb(ctx, pem_passwd_cb);
	SSL_CTX_set_default_passwd_cb_userdata(ctx, "1234567");

	//setup private key
	//might need to split cert and key files
	SSL_CTX_use_PrivateKey_file(ctx, CERTFILE, SSL_FILETYPE_PEM);
	SSL_check_private_key(ssl);
	//need additional private key / public key setting?
	
	//TODO: is further private key loading for the SSL (not SSL_CTX) needed?

	//load trusted CA
	//block used file as .b64 and now it is .pem, changes?
	if (!SSL_CTX_load_verify_locations(ctx, CA_CERT, NULL)) {
		ERR_print_errors_fp(stderr);
		printf("exiting due to CA failure\n");
		exit(1);
	}
	ERR_print_errors_fp(stderr);


	sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	CHK_ERR(sock, "socket");

	memset(&server_addr, '\0', sizeof(server_addr));
	server_addr.sin_family = AF_INET;
	server_addr.sin_port = htons(s_port);

	server_addr.sin_addr.s_addr = inet_addr(s_ipaddr);

	err = connect(sock, (struct sockaddr*) &server_addr, sizeof(server_addr));
	CHK_ERR(err, "connect");

	printf("connected");

	ssl = SSL_new(ctx);
	SSL_set_fd(ssl, sock);
	err = SSL_connect(ssl);
	SSL_set_connect_state(ssl);
	peer_cert = SSL_get_peer_certificate(ssl);
	ERR_print_errors_fp(stderr);


	




	err = SSL_write(ssl, wbuf, strlen(wbuf));
	if (err <= 0) {
		printf("%d: %s\n", SSL_get_error(ssl, err), ERR_error_string(err, NULL));
	}
	CHK_ERR(err, "write");

	err = SSL_read(ssl, wbuf, sizeof(wbuf) - 1);
	CHK_ERR(err, "read");
	ERR_print_errors_fp(stderr);

	SSL_shutdown(ssl);

	//write buffer to file


	return 0;
}
