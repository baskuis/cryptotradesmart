#!/bin/bash
ps aux | grep [t]radestudent
if [ $? -eq 1 ]; then
    echo "respawning" | tee -a /var/local/respawn.out
    /var/local/start.sh
else
    echo "already running" | tee -a /var/local/respawn.out
fi