package core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public final class FileManager {

    private FileManager() {

    }

    public static String[] readAllLines(File file) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            if (!file.exists()) file.createNewFile();

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
        if (!file.exists()) file.mkdir();
        return file.listFiles();
    }

    public static File loadAndCreateDirectory(String filepath) {
        File file = new File(filepath);
        if (!file.exists()) file.mkdir();
        return file;
    }

    public static File loadAndCreateFile(String filepath) {
        File file = new File(filepath);
        try {
            if (!file.exists()) file.createNewFile();
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
}
