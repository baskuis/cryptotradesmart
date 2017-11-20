cd /var/local
ps aux | grep [t]radestudent-learn | kill -9 `awk '{print $2}'`
sleep 2
/usr/bin/nohup /usr/bin/java -jar tradestudent-learn-0.0.1-SNAPSHOT.jar -Xms512m -Xmx1024m &