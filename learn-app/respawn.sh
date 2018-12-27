#!/bin/bash
ps aux | grep [t]radestudent
if [ $? -eq 1 ]; then
    echo "respawning" | tee -a /home/bas/Projects/tradestudent-learn/respawn.out
    /home/bas/Projects/tradestudent-learn/start.sh
else
    echo "already running" | tee -a /home/bas/Projects/tradestudent-learn/respawn.out
fi