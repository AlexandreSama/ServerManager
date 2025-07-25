package fr.djinn.servermanager;

import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerApiHttp extends NanoHTTPD {

    private final String authToken;
    private final ServerManager plugin;

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

        String uri = session.getUri();
        Method method = session.getMethod();

        // --- GET /api/serverinfo
        if (uri.equalsIgnoreCase("/api/serverinfo") && method == Method.GET) {
            int players = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            String json = String.format("{ \"players\": %d, \"maxPlayers\": %d }", players, max);
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }

        // --- GET /api/status
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

        // --- GET /api/players
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

        // --- POST /api/stop
        if (uri.equalsIgnoreCase("/api/stop") && method == Method.POST) {
            return handlePostWithUsername(session, (username, params) -> {
                plugin.getLogger().warning("🛑 Arrêt demandé par " + username);
                plugin.getServer().getScheduler().runTask(plugin, Bukkit::shutdown);
                return "Arrêt du serveur lancé.";
            });
        }

        // --- POST /api/restart
        if (uri.equalsIgnoreCase("/api/restart") && method == Method.POST) {
            return handlePostWithUsername(session, (username, params) -> {
                plugin.getLogger().warning("♻️ Redémarrage demandé par " + username);
                plugin.getServer().getScheduler().runTask(plugin, Bukkit::shutdown);
                return "Redémarrage du serveur lancé.";
            });
        }

        // --- POST /api/message
        if (uri.equalsIgnoreCase("/api/message") && method == Method.POST) {
            return handlePostWithUsername(session, (username, params) -> {
                String text = getFirstParam(params, "text");
                if (text == null || text.isEmpty()) return "Paramètre 'text' requis.";
                Bukkit.broadcastMessage("§a[" + username + "] §f" + text);
                plugin.getLogger().info("📢 Message envoyé par " + username + " : " + text);
                return "Message envoyé.";
            });
        }

        // --- POST /api/kick
        if (uri.equalsIgnoreCase("/api/kick") && method == Method.POST) {
            return handlePostWithUsername(session, (username, params) -> {
                String target = getFirstParam(params, "player");
                String reason = getFirstParam(params, "reason", "Expulsé via l'API");
                String silent = getFirstParam(params, "silent", "false");

                if (target == null) return "Paramètre 'player' requis.";
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

        // --- POST /api/command
        if (uri.equalsIgnoreCase("/api/command") && method == Method.POST) {
            return handlePostWithUsername(session, (username, params) -> {
                String cmd = getFirstParam(params, "cmd");
                if (cmd == null || cmd.isEmpty()) return "Paramètre 'cmd' requis.";
                plugin.getLogger().info("⚙️ [" + username + "] a exécuté : /" + cmd);
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd));
                return "Commande exécutée.";
            });
        }

        // --- GET /api/chatlog
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

    private String getFirstParam(Map<String, List<String>> params, String key) {
        return getFirstParam(params, key, null);
    }

    private String getFirstParam(Map<String, List<String>> params, String key, String defaultValue) {
        List<String> values = params.get(key);
        return (values != null && !values.isEmpty()) ? values.getFirst() : defaultValue;
    }

    private String requireUsername(Map<String, List<String>> parameters) {
        String username = getFirstParam(parameters, "username");
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Paramètre 'username' requis.");
        }
        return username;
    }

    @FunctionalInterface
    private interface PostHandler {
        String handle(String username, Map<String, List<String>> parameters) throws Exception;
    }

    private Response handlePostWithUsername(IHTTPSession session, PostHandler handler) {
        try {
            session.parseBody(new HashMap<>());
            Map<String, List<String>> parameters = session.getParameters();
            String username = requireUsername(parameters);
            String result = handler.handle(username, parameters);
            return newFixedLengthResponse(Response.Status.OK, "text/plain", result);
        } catch (IllegalArgumentException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Erreur serveur : " + e.getMessage());
        }
    }

}