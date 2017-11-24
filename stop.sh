ps aux | grep [t]radestudent-learn | kill -9 `awk '{print $2}'`
echo "stopped"