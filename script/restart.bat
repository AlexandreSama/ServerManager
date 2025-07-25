@echo off
set JAR_FILE=paper-1.21.1.jar
set MEMORY=4G

:restart
echo ➡️ Démarrage du serveur...
java -Xmx%MEMORY% -jar %JAR_FILE% nogui

echo 🔁 Le serveur s'est arrêté. Redémarrage dans 5 secondes...
timeout /t 5
goto restart
