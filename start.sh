#!/bin/bash

while getopts 'dt' OPTION; do
  case "$OPTION" in
    d)
      docker-compose up -d db && docker ps
       ;;
    t)
      docker-compose up -d test-db && docker ps
      ;;
    ?)
      echo "Usage: $(basename $0) [-d] [-t]"
      exit 1
      ;;
  esac
done
