#!/bin/bash

# Nom du fichier .jar de ton serveur
JAR_FILE="paper-1.21.1.jar"

# Mémoire allouée
MEMORY="4G"

# Boucle de redémarrage
while true; do
    echo "➡️ Démarrage du serveur..."
    java -Xmx$MEMORY -jar $JAR_FILE nogui

    echo "🔁 Le serveur s'est arrêté. Redémarrage dans 5 secondes..."
    sleep 5
done
