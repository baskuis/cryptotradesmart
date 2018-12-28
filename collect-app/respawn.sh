ps aux | grep [c]ollect
if [ $? -eq 1 ]; then
    echo "respawning collector" | tee -a /home/bas/Projects/tradestudent-learn/respawn.out
    /home/bas/Projects/tradestudent-learn/collect-app/start.sh
else
    echo "collect app already running" | tee -a /home/bas/Projects/tradestudent-learn/respawn.out
fi
