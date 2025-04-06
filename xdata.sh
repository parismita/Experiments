#!/bin/bash

if [ "$1" == "build" ]; then
  docker compose build
  docker compose up -d
elif [ "$1" == "run" ]; then
  docker exec -it xdata3 bash
  docker compose stop
else
  echo "Invalid argument. Please use 'build' or 'run'."
fi
