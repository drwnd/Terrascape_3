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
}
