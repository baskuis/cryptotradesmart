#!/usr/bin/env bash
/usr/bin/nohup /usr/bin/java -jar /home/bas/Projects/tradestudent-learn/learn-app/build/libs/learn-app.jar -Xms12000m -Xmx24000m -XX:-UseGCOverheadLimit >/home/bas/Projects/tradestudent-learn/learn-app/application.out 2>&1 &
