#!/usr/bin/env bash
/usr/bin/nohup /usr/bin/java -jar /home/bas/Projects/tradestudent-learn/build/libs/tradestudent-learn-0.0.1-SNAPSHOT.jar -Xms12000m -Xmx24000m -XX:-UseGCOverheadLimit >/home/bas/Projects/tradestudent-learn/application.out 2>&1 &