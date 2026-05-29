package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Logger {
    public static void log(String msg) {
        log(msg, new File("depths.txt"));
    }

    public static void log(String msg, File file) {
        if (!file.exists()) {
            IO.println("Creating file " + file.getName());
            try {
                if (!file.createNewFile())
                    IO.println("Failed to create file " + file.getName());
            } catch (IOException e) {
                IO.println("Failed to create file " + file.getName());
            }
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            writer.println(msg);
        } catch (IOException e) {
            IO.println("Failed to append to file " + file.getName());
        }
    }
}
