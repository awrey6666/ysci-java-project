#!/usr/bin/env bash
# Minimal wait-for-it to wait for TCP host:port
set -e
hostport="$1"
shift
if [ -z "$hostport" ]; then
  echo "Usage: $0 host:port [-- command]"
  exit 1
fi
host=${hostport%%:*}
port=${hostport##*:}
timeout=${WAIT_FOR_TIMEOUT:-60}

echo "Waiting for $host:$port (timeout ${timeout}s)"
for i in $(seq 1 ${timeout}); do
  if (echo > /dev/tcp/$host/$port) >/dev/null 2>&1; then
    echo "$host:$port is available"
    if [ "$#" -gt 0 ]; then
      exec "$@"
    else
      exit 0
    fi
  fi
  sleep 1
done
echo "Timeout waiting for $host:$port"
exit 1
