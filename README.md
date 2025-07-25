<p align="center">
  <img src="plugin-icon.png" width="96" height="96" alt="Keystone logo"/>
</p>

# Keystone Plugin

Keystone est un plugin Spigot sécurisé permettant de contrôler un serveur Minecraft via une API HTTP/HTTPS.

## 🚀 Fonctionnalités

- 🔐 Authentification par token + signature HMAC optionnelle
- 🌐 API REST en JSON
- 📱 Compatible avec application mobile externe
- ✅ Endpoints sécurisés (commande, message, kick, etc.)
- 🔒 HTTPS via keystore configurable
- 🧪 Validation automatique des paramètres JSON

## 📦 Endpoints

| Méthode | URL                | Description                                 |
|---------|--------------------|---------------------------------------------|
| GET     | `/api/serverinfo`  | Joueurs connectés + slots max               |
| GET     | `/api/status`      | Statut mémoire et nombre de joueurs         |
| GET     | `/api/players`     | Liste des joueurs avec UUID et ping         |
| GET     | `/api/chatlog`     | Historique du chat récent                   |
| POST    | `/api/message`     | Envoie un message aux joueurs               |
| POST    | `/api/broadcast`   | Message chat + console                      |
| POST    | `/api/command`     | Exécute une commande console                |
| POST    | `/api/kick`        | Expulse un joueur avec raison               |
| POST    | `/api/stop`        | Arrêt du serveur                            |
| POST    | `/api/restart`     | Redémarrage du serveur                      |
| POST    | `/api/playerinfo`  | Détails d’un joueur (vie, position, etc.)   |

## ⚙️ Configuration (`config.yml`)

```yaml
api:
  enabled: true
  port: 8443
  token: votretokenici
  ssl: true
  keystore:
    path: plugins/Keystone/keystore.jks
    password: changeit
  hmac:
    enabled: true
    secret: votreSecretIci
```

## 🔧 Générer un keystore

```bash
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -keystore keystore.jks -validity 365
```

## 🧠 HMAC Signature (optionnelle)

Chaque requête POST peut être signée côté client avec un HMAC SHA-256 utilisant :

```
Méthode + URI + Body + Secret
```

Header attendu :  
`X-Signature: <signature hex>`

## 📱 Application mobile

Une app Android compagnon permet de :

- Voir les joueurs connectés
- Envoyer des messages
- Exécuter des commandes
- Gérer les accès avec pseudo/password

## 🖼️ Icône & Branding

![Keystone Logo](./plugin-icon.png)

## 📄 Licence

MIT © Djinn - 2025
