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

ipa-server-install \
          --realm $REALM \
          --domain $DOMAIN \
          --hostname $FQDN \
          -a $FPW \
          -p $FPW \
          --setup-dns \
          --auto-reverse \
{%- for zone in salt['pillar.get']('freeipa:reverseZones').split(',') %}
          --reverse-zone {{ zone }} \
{%- endfor %}
          --allow-zone-overlap \
          --ssh-trust-dns \
          --mkhomedir \
          --ip-address $IPADDR \
          --auto-forwarders \
          --unattended

# Setup basic CNAME records for pointing to FreeIPA services
echo "$FPW" | kinit admin
ipa dnsrecord-add $DOMAIN kdc --cname-rec="ipa-ca.$DOMAIN."
ipa dnsrecord-add $DOMAIN kerberos --cname-rec="ipa-ca.$DOMAIN."
ipa dnsrecord-add $DOMAIN ldap --cname-rec="ipa-ca.$DOMAIN."
ipa dnsrecord-add $DOMAIN freeipa --cname-rec="ipa-ca.$DOMAIN."

LDAP_CERT_REQUEST_ID=`getCertRequestIdFromDir /etc/dirsrv/slapd-${REALM//./-} Server-Cert`
ipa host-add --force ldap.$DOMAIN
ipa service-add ldap/ldap.$DOMAIN
ipa service-add-host ldap/ldap.$DOMAIN --host $FQDN
ipa-getcert resubmit -D ldap.$DOMAIN -i $LDAP_CERT_REQUEST_ID -w

HTTP_CERT_REQUEST_ID=`getCertRequestIdFromDir /etc/httpd/alias Server-Cert`
ipa host-add --force freeipa.$DOMAIN
ipa service-add HTTP/freeipa.$DOMAIN
ipa service-add-host HTTP/freeipa.$DOMAIN --host $FQDN
ipa host-add --force kdc.$DOMAIN
ipa service-add HTTP/kdc.$DOMAIN
ipa service-add-host HTTP/kdc.$DOMAIN --host $FQDN
ipa host-add --force kerberos.$DOMAIN
ipa service-add HTTP/kerberos.$DOMAIN
ipa service-add-host HTTP/kerberos.$DOMAIN --host $FQDN
ipa-getcert resubmit -D freeipa.$DOMAIN -D kdc.$DOMAIN -D kerberos.$DOMAIN -i $HTTP_CERT_REQUEST_ID -w

kdestroy

set +e
