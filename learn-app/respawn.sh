#!/bin/bash
ps aux | grep [t]radestudent
if [ $? -eq 1 ]; then
    echo "respawning" | tee -a /home/bas/Projects/tradestudent-learn/respawn.out
    /home/bas/Projects/tradestudent-learn/learn-app/start.sh
else
    echo "learn app already running" | tee -a /home/bas/Projects/tradestudent-learn/respawn.out
fi
