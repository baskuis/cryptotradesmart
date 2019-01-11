#!/usr/bin/env bash
/usr/bin/nohup /usr/bin/java -jar /home/bas/Projects/tradestudent-learn/collect-app/build/libs/collect-app.jar -Xms4000m -Xmx7000m -XX:-UseGCOverheadLimit >/home/bas/Projects/tradestudent-learn/collect-app/application.out 2>&1 &
