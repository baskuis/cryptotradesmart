git add . && git commit -m 'khub push' && git pull && git push && ./stopAll.sh && ./gradlew build -xtest && sh collect-app/respawn.sh && sh learn-app/respawn.sh
