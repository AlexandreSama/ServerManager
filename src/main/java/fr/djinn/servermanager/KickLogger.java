package fr.djinn.servermanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class KickLogger {

    private static final File logFile = new File("plugins/ServerManager/kicks.log");

    public static void logKick(String actor, String target, String reason) {
        String time = LocalDateTime.now().toString();
        String entry = String.format("[%s] %s kicked %s - Reason: %s", time, actor, target, reason);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(entry);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture dans kicks.log : " + e.getMessage());
        }
    }
}