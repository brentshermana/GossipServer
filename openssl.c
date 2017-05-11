
#include "openssl/bio.h"
#include "openssl/ssl.h"
#include "openssl/err.h"
#include <stdio.h>
#include <string.h>
#include <errno.h>

#define CERTFILE "client.pem"
#define SERVER debatedecide.fit.edu/proposals
#define PORT 80

/*int pem_passwd_cb(char *buf, int size, int rwflag, void *password)
{
	strncpy(buf, (char*)password, size);
	buf[size-1] = '\0';
	return strlen(buf);
}*/

// configure SSL_CTX

//variables
	BIO *conn = NULL;
	SSL_METHOD *method;
	SSL_CTX *ctx;
	SSL *ssl;
	char buf[1024];
	int err;
SSL_CTX *setup_client_ctx(void)
{
	SSL_CTX *ctx = SSL_CTX_new(SSLv23_client_method());
	if(SSL_CTX_use_certificate_chain_file(ctx, CERTFILE)!= 1)
	{
		//error
	}
	//SSL_CTX_set_default_passwd_cb(ctx, pem_passwd_cb);
	SSL_CTX_set_default_passwd_cb_userdata(ctx, "1234567");
	if(SSL_CTX_use_PrivateKey_file(ctx, CERTFILE, SSL_FILETYPE_PEM)!=1)
	{
		//error
	}
	return ctx;
}

int do_client_task(SSL *ssl)
{
	err = SSL_read(ssl, buf, sizeof(buf) - 1);
	CHK_SSL(err);
	buf[err] = '\0';
	ERR_print_errors_fp(stderr);
	return 1;
}

int main(int argc, char **argv)
{
	
	// Initializing OpenSSL
	SSL_load_error_strings();
	ERR_load_BIO_strings();
	SSL_library_init(); //equals OpenSSL_add_ssl_algorithms(); SSLeay_add_ssl_algorithms();
	
	seed_prng();
	
	//creat SSL method
	method = SSLv23_client_method();
	ctx = setup_client_ctx();
	if(!(conn = BIO_new_connect(SERVER ":" PORT)))
	{
		//error;
	}
	if(BIO_do_connect(conn) <= 0)
	{
		//error
	}
	//creat SSL_CTX
	if(!(ssl = SSL_new(ctx)
	{
		//error
	}
	//connect SSL object and BIO
	SSL_set_bio(ssl, conn, conn);
	
	//SSL handshake
	if(SSL_connect(ssl) <= 0)
	{
		//error
	}
	
	if((err = post_connection_check(ssl, "server")) !=X509_V_OK)
	{
		//error
	}
	printf("SSL connection has been opened");
	
	/*if(ctx == NULL)
	{
		BIO_printf(outbio, "could not initialize the OpenSSL lirary!\n");
	}*/
	
	
	if(do_client_task(ssl))
	{
		SSL_shutdown(ssl);
	}
	else
	{
		SSL_clear(ssl);
	}
	printf("SSL colse");
	SSL_free(ssl);
	SSL_CTX_free(ctx);
	return 0;
}
