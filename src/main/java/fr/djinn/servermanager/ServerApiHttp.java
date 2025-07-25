package fr.djinn.servermanager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fi.iki.elonen.NanoHTTPD;
import fr.djinn.servermanager.security.HmacVerifier;
import fr.djinn.servermanager.util.JsonValidator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.util.*;

public class ServerApiHttp extends NanoHTTPD {

    private final String authToken;
    private final ServerManager plugin;
    private final Gson gson = new Gson();

    public ServerApiHttp(ServerManager plugin, int port, String authToken) {
        super(port);
        this.plugin = plugin;
        this.authToken = authToken;
    }

    public void startSecure(File keystoreFile, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            ks.load(fis, password.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password.toCharArray());

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(kmf.getKeyManagers(), null, null);

        makeSecure(sc.getServerSocketFactory(), null);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String authHeader = headers.get("authorization");

        if (authHeader == null || !authHeader.equals("Bearer " + authToken)) {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "401 Unauthorized");
        }

        String secret = plugin.getConfig().getString("api.hmac.secret");
        boolean hmacEnabled = plugin.getConfig().getBoolean("api.hmac.enabled", false);

        String body = getRequestBody(session);

        if (hmacEnabled && session.getMethod() != Method.GET) {
            if (!HmacVerifier.isValidSignature(session.getMethod().name(), session.getUri(), headers, body, secret)) {
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "HMAC signature invalide");
            }
        }

        String uri = session.getUri();
        Method method = session.getMethod();

        if (uri.equalsIgnoreCase("/api/serverinfo") && method == Method.GET) {
            int players = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            String json = String.format("{ \"players\": %d, \"maxPlayers\": %d }", players, max);
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }

        if (uri.equalsIgnoreCase("/api/status") && method == Method.GET) {
            Runtime rt = Runtime.getRuntime();
            long usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long maxMem = rt.maxMemory() / (1024 * 1024);
            int players = Bukkit.getOnlinePlayers().size();
            int maxPlayers = Bukkit.getMaxPlayers();
            String json = String.format("{ \"players\": %d, \"maxPlayers\": %d, \"memoryUsedMB\": %d, \"memoryMaxMB\": %d }",
                    players, maxPlayers, usedMem, maxMem);
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }

        if (uri.equalsIgnoreCase("/api/players") && method == Method.GET) {
            StringBuilder json = new StringBuilder("[");
            for (Player player : Bukkit.getOnlinePlayers()) {
                json.append(String.format("{ \"name\": \"%s\", \"uuid\": \"%s\", \"ping\": %d },",
                        player.getName(), player.getUniqueId(), player.getPing()));
            }
            if (json.length() > 1) json.setLength(json.length() - 1);
            json.append("]");
            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
        }

        if (uri.equalsIgnoreCase("/api/playerinfo") && method == Method.POST) {
            return handleJson(session, body, (username, data) -> {
                JsonValidator v = new JsonValidator(data);
                v.require("player");
                if (v.hasErrors()) return v.getErrorSummary();

                String target = data.get("player");
                Player player = Bukkit.getPlayerExact(target);
                if (player == null) return "Joueur introuvable ou hors ligne.";

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("uuid", player.getUniqueId().toString());
                info.put("name", player.getName());
                info.put("health", player.getHealth());
                info.put("food", player.getFoodLevel());
                info.put("ping", player.getPing());
                info.put("gamemode", player.getGameMode().name());
                info.put("isOp", player.isOp());

                Location loc = player.getLocation();
                Map<String, Object> locMap = new HashMap<>();
                locMap.put("world", loc.getWorld().getName());
                locMap.put("x", loc.getX());
                locMap.put("y", loc.getY());
                locMap.put("z", loc.getZ());
                info.put("location", locMap);

                List<String> inventory = new ArrayList<>();
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null) inventory.add(item.getType().name());
                }
                info.put("inventory", inventory);

                return gson.toJson(info);
            });
        }

        if (uri.equalsIgnoreCase("/api/stop") && method == Method.POST) {
            return handleJson(session, body, (username, data) -> {
                plugin.getLogger().warning("🛑 Arrêt demandé par " + username);
                plugin.getServer().getScheduler().runTask(plugin, Bukkit::shutdown);
                return "Arrêt du serveur lancé.";
            });
        }

        if (uri.equalsIgnoreCase("/api/restart") && method == Method.POST) {
            return handleJson(session, body, (username, data) -> {
                plugin.getLogger().warning("♻️ Redémarrage demandé par " + username);
                plugin.getServer().getScheduler().runTask(plugin, Bukkit::shutdown);
                return "Redémarrage du serveur lancé.";
            });
        }

        if (uri.equalsIgnoreCase("/api/message") && method == Method.POST) {
            return handleJson(session, body, (username, data) -> {
                JsonValidator v = new JsonValidator(data);
                v.require("text");
                if (v.hasErrors()) return v.getErrorSummary();

                String text = data.get("text");
                Bukkit.broadcastMessage("§a[" + username + "] §f" + text);
                plugin.getLogger().info("📢 Message envoyé par " + username + " : " + text);
                return "Message envoyé.";
            });
        }

        if (uri.equalsIgnoreCase("/api/kick") && method == Method.POST) {
            return handleJson(session, body, (username, data) -> {
                JsonValidator v = new JsonValidator(data);
                v.require("player").optional("silent").asBoolean().optional("reason");
                if (v.hasErrors()) return v.getErrorSummary();

                String target = data.get("player");
                String reason = data.getOrDefault("reason", "Expulsé via l'API");
                String silent = data.getOrDefault("silent", "false");

                Player player = Bukkit.getPlayerExact(target);
                if (player == null) return "Joueur introuvable ou hors ligne.";

                player.kickPlayer(reason);
                String log = String.format("👢 [%s] %s a été expulsé. Raison : %s", username, target, reason);

                plugin.getLogger().info(log);
                KickLogger.logKick(username, target, reason);

                if (!silent.equalsIgnoreCase("true")) {
                    Bukkit.broadcastMessage("§c" + log);
                }

                return "Joueur expulsé.";
            });
        }

        if (uri.equalsIgnoreCase("/api/command") && method == Method.POST) {
            return handleJson(session, body, (username, data) -> {
                JsonValidator v = new JsonValidator(data);
                v.require("cmd");
                if (v.hasErrors()) return v.getErrorSummary();

                String cmd = data.get("cmd");
                plugin.getLogger().info("⚙️ [" + username + "] a exécuté : /" + cmd);
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd));
                return "Commande exécutée.";
            });
        }

        if (uri.equalsIgnoreCase("/api/chatlog") && method == Method.GET) {
            List<String> logs = ChatLogger.getLastMessages();
            StringBuilder json = new StringBuilder("[");
            for (String msg : logs) {
                json.append("\"").append(msg.replace("\"", "\\\"")).append("\",");
            }
            if (json.length() > 1) json.setLength(json.length() - 1);
            json.append("]");
            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
    }

    private String getRequestBody(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            return files.get("postData") != null ? files.get("postData") : "";
        } catch (Exception e) {
            return "";
        }
    }

    private Response handleJson(IHTTPSession session, String body, JsonHandler handler) {
        try {
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> data = gson.fromJson(body, mapType);
            if (data == null || !data.containsKey("username")) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Paramètre 'username' requis.");
            }
            String username = data.get("username");
            String result = handler.handle(username, data);
            return newFixedLengthResponse(Response.Status.OK, "text/plain", result);
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Erreur serveur : " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface JsonHandler {
        String handle(String username, Map<String, String> data) throws Exception;
    }
}

