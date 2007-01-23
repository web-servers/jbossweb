To install it copy the jar file (valves.jar) in server/lib
And add the following in server.xml:
+++
<Valve className="SSLValve"/>
+++
And use something like the following in httpd.conf:
+++
<IfModule ssl_module>
RequestHeader set SSL_CLIENT_CERT "%{SSL_CLIENT_CERT}s"
RequestHeader set SSL_CIPHER "%{SSL_CIPHER}s"
RequestHeader set SSL_SESSION_ID "%{SSL_SESSION_ID}s"
RequestHeader set SSL_CIPHER_USEKEYSIZE "%{SSL_CIPHER_USEKEYSIZE}s"
</IfModule>
+++
