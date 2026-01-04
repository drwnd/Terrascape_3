package core.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public final class FileManager {

    private FileManager() {

    }

    public static String[] readAllLines(File file) {
        ArrayList<String> lines = new ArrayList<>();
        file = loadAndCreateFile(file.getPath());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file.getPath()));
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                lines.add(line);
            }
            reader.close();
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

    public static File[] getChildren(File file) {
        file.mkdirs();
        return file.listFiles();
    }

    public static File loadAndCreateDirectory(String filepath) {
        File file = new File(filepath);
        file.mkdirs();
        return file;
    }

    public static File loadAndCreateFile(String filepath) {
        File file = new File(filepath);
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
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

    public static String loadFileContents(String filepath) {
        String result;

        try {
            InputStream in = new FileInputStream(filepath);
            Scanner scanner = new Scanner(in, StandardCharsets.UTF_8);
            result = scanner.useDelimiter("\\A").next();

        } catch (FileNotFoundException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }

    public static String loadJson(String filepath) {
        File file = new File(filepath);
        if (!file.exists()) return "{}";

        InputStream in;
        try {
            in = new FileInputStream(filepath);
        } catch (FileNotFoundException _) {
            return "{}";
        }
        Scanner scanner = new Scanner(in, StandardCharsets.UTF_8);
        return scanner.useDelimiter("\\A").next();
    }
}
