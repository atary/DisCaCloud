package org.cloudbus.cloudsim.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Anup
 *
 */
public class PrintFile {

    public static String file_name = "";

    public static void AddtoFile(String msg) {
        try {
            java.util.Date d = new java.util.Date();
            if ("".equals(file_name)) {
                file_name = "c:/log/cloudSim_Log" + d.getTime() + ".txt";
            }
            File file = new File(file_name);
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileWriter fw = new FileWriter(file.getAbsoluteFile(), true)) {
                fw.write(msg);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}
