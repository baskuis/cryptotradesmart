./gradlew clean build
scp build/libs/tradestudent-learn-0.0.1-SNAPSHOT.jar root@104.236.224.214:/var/local
ssh root@104.236.224.214 `./var/local/stop.sh`