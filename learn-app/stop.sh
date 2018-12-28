ps aux | grep [t]radestudent-learn | grep [l]earn-app | kill -9 `awk '{print $2}'`
echo "stopped"
