./gradlew clean build -xtest
scp build/libs/tradestudent-learn-0.0.1-SNAPSHOT.jar root@165.227.18.245:/var/local
ssh root@165.227.18.245 /var/local/restart.sh
