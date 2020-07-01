#!/usr/bin/env bash

set -x

mvn compile exec:exec -Dexec.args="-classpath %classpath com.reactivebbq.orders.LoadTest"
