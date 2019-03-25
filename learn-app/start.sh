#!/usr/bin/env bash
/usr/bin/nohup /usr/bin/java -jar /home/bas/Projects/tradestudent-learn/learn-app/build/libs/learn-app-0.0.1-SNAPSHOT.jar -Xms12000m -Xmx24000m -XX:-UseGCOverheadLimit >/home/bas/Projects/tradestudent-learn/learn-app/application.out 2>&1 &
