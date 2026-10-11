#!/bin/bash
set -euo pipefail
runtime=/opt/jboss/wildfly/standalone/data/agent-engine
install -d -m 700 -o jboss -g root "$runtime" "$runtime/secrets"
for source in /run/secrets/*; do
  install -m 600 -o jboss -g root "$source" "$runtime/secrets/$(basename "$source")"
done
export TLS_PASSWORD="$(cat "$runtime/secrets/tls.password")"
export SPRING_CONFIG_IMPORT="configtree:$runtime/secrets/"
export SIGNING_KEY_PATH="$runtime/signing.jwk"
exec /usr/sbin/runuser -u jboss -- /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 127.0.0.1
