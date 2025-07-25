package fr.djinn.servermanager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ServerManager extends JavaPlugin {

    private ServerApiHttp apiServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Vérifie que la section "api" est présente
        if (!getConfig().isConfigurationSection("api")) {
            logError("La section 'api' est absente de config.yml !");
            logError("Ajoutez-la ou supprimez ce plugin si vous ne souhaitez pas utiliser l'API.");
            disablePlugin();
            return;
        }

        if (!getConfig().getBoolean("api.enabled", true)) {
            getLogger().info("API désactivée dans la configuration.");
            return;
        }

        String token = getConfig().getString("api.token");
        int port = getConfig().getInt("api.port", 8443);
        boolean sslEnabled = getConfig().getBoolean("api.ssl", true);

        try {
            apiServer = new ServerApiHttp(this, port, token);

            if (sslEnabled) {
                String keystorePath = getConfig().getString("api.keystore.path");
                String keystorePassword = getConfig().getString("api.keystore.password");

                if (keystorePath == null || keystorePath.isEmpty()) {
                    logError("Chemin vers le keystore vide ! Il faut l'ajouter dans la configuration.");
                    disablePlugin();
                    return;
                }

                File ksFile = new File(keystorePath);
                if (!ksFile.exists()) {
                    logError("❌ Keystore introuvable à l'emplacement : " + ksFile.getAbsolutePath());
                    logError("🔐 Générez-en un avec :");
                    logError("keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -keystore keystore.jks -validity 365");
                    disablePlugin();
                    return;
                }

                if (keystorePassword == null || keystorePassword.isEmpty()) {
                    logError("Mot de passe du keystore vide !");
                    disablePlugin();
                    return;
                }

                Bukkit.getPluginManager().registerEvents(new ChatLogger(), this);
                apiServer.startSecure(ksFile, keystorePassword);
                getLogger().info("✅ API HTTPS démarrée sur le port " + port);
            } else {
                Bukkit.getPluginManager().registerEvents(new ChatLogger(), this);
                apiServer.start(); // HTTP non sécurisé (déconseillé)
                getLogger().warning("⚠️ API démarrée sans SSL sur le port " + port);
            }

        } catch (Exception e) {
            logError("Impossible de démarrer l'API : " + e.getMessage());
            e.printStackTrace();
            disablePlugin();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ServerManager désactivé !");
        if (apiServer != null) {
            apiServer.stop();
        }
    }

    private void logError(String msg) {
        getLogger().severe("🔐 [ServerManager] " + msg);
    }

    private void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }
}