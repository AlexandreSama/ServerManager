<p align="center">
  <img src="plugin-icon.png" width="96" height="96" alt="Keystone logo"/>
</p>

# 🔐 Keystone - ServerManager API Plugin

Keystone est un plugin Minecraft pour Spigot 1.21+ qui permet de contrôler votre serveur via une API HTTPS sécurisée (token + SSL + audit log).

## ✨ Fonctionnalités

- API REST sécurisée (HTTPS + Token + username)
- Redémarrage, arrêt, message global, kick, commandes à distance
- Intégration mobile ou frontend
- Journalisation des actions avec trace utilisateur

## 🔧 Configuration

```yaml
api:
  enabled: true
  port: 8443
  token: CHANGEMOI
  ssl: true
  keystore:
    path: keystore.jks
    password: CHANGEMOI
```

## 🚀 Endpoints disponibles

| Méthode | Endpoint           | Description                      |
|---------|--------------------|----------------------------------|
| GET     | `/api/serverinfo`  | Infos joueurs                    |
| GET     | `/api/status`      | RAM + slots                      |
| POST    | `/api/stop`        | Arrêt                            |
| POST    | `/api/restart`     | Redémarrage                      |
| GET     | `/api/players`     | Liste joueurs connectés          |
| POST    | `/api/message`     | Message global                   |
| POST    | `/api/kick`        | Expulsion personnalisée          |
| POST    | `/api/command`     | Exécute une commande             |
| GET     | `/api/chatlog`     | Logs récents du chat             |

## 🔐 Authentification

- Header obligatoire : `Authorization: Bearer VOTRE_TOKEN`
- Tous les endpoints POST exigent `username` (identité humaine pour trace)

## 📦 Déploiement

1. Place le plugin dans `/plugins`
2. Configure ton SSL avec `keytool`
3. Démarre ton serveur Spigot

## 👨‍💻 Auteurs

Made by **Djinn** with love ❤️
