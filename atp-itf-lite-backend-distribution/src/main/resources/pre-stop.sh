#!/usr/bin/env sh

xargs -rt -a /atp-itf-lite/application.pid kill -SIGTERM
sleep 29
