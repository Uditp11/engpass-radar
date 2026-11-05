#!/usr/bin/env sh
# Baut das Projekt mit dem Maven Wrapper und startet es auf Payara Micro.
set -e
cd "$(dirname "$0")"
./mvnw -B -q package
echo
echo "Engpass-Radar startet auf http://localhost:8080/"
echo
exec java -Dhazelcast.phone.home.enabled=false -jar target/payara-micro.jar --deploy target/engpass-radar.war --contextroot / --port 8080 --nocluster
