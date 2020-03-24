#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

getCertRequestIdFromDir() {
  CERT_DIR=$1
  NICKNAME=$2

  ipa-getcert list -d ${CERT_DIR} -n ${NICKNAME} | grep "Request ID" | cut -d\' -f2
}

getCertRequestIdFromFile() {
  FILE=$1

  ipa-getcert list -f ${FILE} | grep "Request ID" | cut -d\' -f2
}

ipa-replica-install \
          --server $FREEIPA_TO_REPLICATE \
          --setup-ca \
          --realm $REALM \
          --domain $DOMAIN \
          --hostname $FQDN \
          --principal $ADMIN_USER \
          --admin-password $FPW \
          --setup-dns \
          --auto-reverse \
          --allow-zone-overlap \
          --ssh-trust-dns \
          --mkhomedir \
          --ip-address $IPADDR \
          --auto-forwarders \
          --unattended

echo "$FPW" | kinit admin

LDAP_CERT_REQUEST_ID=`getCertRequestIdFromDir /etc/dirsrv/slapd-${REALM//./-} Server-Cert`
ipa service-add-host ldap/ldap.$DOMAIN --host $FQDN
ipa-getcert resubmit -D ldap.$DOMAIN -i $LDAP_CERT_REQUEST_ID -w

HTTP_CERT_REQUEST_ID=`getCertRequestIdFromDir /etc/httpd/alias Server-Cert`
ipa service-add-host HTTP/freeipa.$DOMAIN --host $FQDN
ipa service-add-host HTTP/kdc.$DOMAIN --host $FQDN
ipa service-add-host HTTP/kerberos.$DOMAIN --host $FQDN
ipa-getcert resubmit -D freeipa.$DOMAIN -D kdc.$DOMAIN -D kerberos.$DOMAIN -i $HTTP_CERT_REQUEST_ID -w

kdestroy

set +e
