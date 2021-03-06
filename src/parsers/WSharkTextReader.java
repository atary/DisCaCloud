/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author ovatman
 */
public class WSharkTextReader implements RequestTextReaderInterface {

    private Scanner fileScan;

    public ArrayList<RequestDatum> read(String aFileName) throws FileNotFoundException, ParseException {
        ArrayList<RequestDatum> readRecords = new ArrayList<RequestDatum>();

        File file = new File(aFileName);

        if (file.isFile()) {
            readAllLines(file, readRecords);
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (f.getName().endsWith(".log")) {
                    readAllLines(f, readRecords);
                }
            }
        }

        return readRecords;

    }

    private void readAllLines(File f, ArrayList<RequestDatum> readRecords) throws FileNotFoundException, ParseException {
        Scanner s = new Scanner(f);
        String line;

        while (s.hasNext()) {
            line = s.nextLine().trim().replaceAll(" +", " ");
            if (line.length() > 0 && Character.isDigit(line.charAt(0))) {
                readRecords.add(WSharkDatum.parseWSharkDatum(line));
            }
        }

    }

    void dumpAll(String aFileName, PrintWriter pw) throws FileNotFoundException, ParseException {
        File file = new File(aFileName);

        if (file.isFile() && !file.toPath().toString().contains("juice")) {
            dumpAllLines(file, pw);
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (!f.toPath().toString().contains("juice")) {
                    dumpAllLines(f, pw);
                }
            }
        }

    }

    private void dumpAllLines(File file, PrintWriter pw) throws FileNotFoundException, ParseException {
        Scanner s = new Scanner(file);
        WSharkDatum temp = null;
        String tempstr, line;
        int counter = 0;

        while (s.hasNext()) {
            line = s.nextLine().trim().replaceAll(" +", " ");
            if (line.length() > 0 && Character.isDigit(line.charAt(0))) {
                try {
                    temp = WSharkDatum.parseWSharkDatum(line);
                } catch (Exception e) {
                    System.out.println(counter);
                    continue;
                }
                counter++;
                tempstr = "" + temp.getReqTime() + "\t" + temp.getClientID() + "\t" + temp.getServerID() + "\t" + temp.getLength() + "\n";
                pw.append(tempstr);
            }
        }
    }

    public void open(String fName) throws FileNotFoundException {
        fileScan = new Scanner(new File(fName));
    }

    public boolean hasNext() {
        return fileScan.hasNext();
    }

    public List<RequestDatum> readNRecords(int limit) {
        LinkedList<RequestDatum> wsList = new LinkedList<>();
        int counter = 0;

        while (fileScan.hasNext() && counter++ < limit) {
            wsList.add(new WSharkDatum(fileScan.nextLine().trim()));
        }

        return wsList;
    }

}
