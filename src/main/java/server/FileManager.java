package server;

import org.joml.Vector3f;
import org.joml.Vector3i;
import player.Player;
import server.generation.WorldGeneration;
import utils.Position;
import utils.Utils;

import java.io.File;
import java.io.FileInputStream;

public final class FileManager {

    public static Player loadPlayer() {
        return new Player(new Position(new Vector3i(), new Vector3f()));
    }

    public static Server loadServer() {
        return new Server(0);
    }

    public static void loadUniversalFiles(File saveFile) {
        if (!saveFile.exists()) //noinspection ResultOfMethodCallIgnored
            saveFile.mkdirs();

        chunksFile = new File(saveFile.getPath() + CHUNKS_PATH);
        if (!chunksFile.exists()) //noinspection ResultOfMethodCallIgnored
            chunksFile.mkdirs();

        settingsFile = new File(saveFile.getPath() + SETTINGS_PATH);
        loadWorldSettings();
    }

    private static void loadWorldSettings() {
        try {
            if (!settingsFile.exists()) {
                settingsFile.createNewFile();
                return;
            }
            FileInputStream reader = new FileInputStream(settingsFile.getPath());
            byte[] bytes = reader.readAllBytes();
            reader.close();

            WorldGeneration.SEED = Utils.getLong(bytes, 0);

        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }

    private static File chunksFile, settingsFile;

    private static final String CHUNKS_PATH = "/chunks";
    private static final String SETTINGS_PATH = "/settings";

    private FileManager() {
    }
}
