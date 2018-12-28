ps aux | grep [t]radestudent-learn | grep [c]ollect-app | kill -9 `awk '{print $2}'`
echo "stopped"
