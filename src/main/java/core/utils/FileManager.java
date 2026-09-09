package core.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class FileManager {

    private FileManager() {

    }

    public static String[] readAllLines(Path filepath) {
        ArrayList<String> lines = new ArrayList<>();
        File file = loadAndCreateFile(filepath);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                lines.add(line);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            return new String[0];
        }

        String[] array = new String[lines.size()];
        for (int index = 0; index < array.length; index++) array[index] = lines.get(index);

        return array;
    }

    public static File[] getSiblings(File file) {
        File parent = file.getParentFile();
        return parent.listFiles();
    }

    public static int indexOf(File file, File[] files) {
        for (int index = 0; index < files.length; index++) if (file.equals(files[index])) return index;
        return -1;
    }

    public static File[] getChildren(Path filepath) {
        File file = filepath.toFile();
        File[] children = file.listFiles();
        return children == null ? new File[0] : children;
    }

    public static File loadAndCreateDirectory(Path filepath) {
        File file = filepath.toFile();
        file.mkdirs();
        return file;
    }

    public static File loadAndCreateFile(Path filepath) {
        File file = filepath.toFile();
        try {
            if (!file.exists()) {
                File parent = file.toPath().toAbsolutePath().getParent().toFile();
                parent.mkdirs();
                file.createNewFile();
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return file;
    }

    public static void delete(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        file.delete();
    }

    public static String loadFileContents(Path filepath) {
        try (Scanner scanner = new Scanner(filepath, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        } catch (IOException | NoSuchElementException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static String loadJson(Path filepath) {
        File file = filepath.toFile();
        if (!file.exists()) return "{}";

        try (Scanner scanner = new Scanner(filepath, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        } catch (IOException | NoSuchElementException _) {
            return "{}";
        }
    }
}
