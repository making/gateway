#!/bin/bash

kapp deploy -a gateway -f <(kbld -f k8s) -c
