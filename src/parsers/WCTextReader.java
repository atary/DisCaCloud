/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author ovatman
 */
public class WCTextReader {

    private Scanner fileScan;
    
    public ArrayList<WCDatum> read(String aFileName) throws FileNotFoundException, ParseException {
        ArrayList<WCDatum> readRecords = new ArrayList<WCDatum>();

        File file = new File(aFileName);

        if (file.isFile()) {
            readAllLines(file, readRecords);
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                readAllLines(f, readRecords);
            }
        }

        return readRecords;

    }

    private void readAllLines(File f, ArrayList<WCDatum> readRecords) throws FileNotFoundException, ParseException {
        Scanner s = new Scanner(f);

        while (s.hasNext()) {
            readRecords.add(WCDatum.parseWCDatum(s.nextLine().trim()));
        }

    }

    void dumpAll(String aFileName, PrintWriter pw, long offSet) throws FileNotFoundException, ParseException {
        File file = new File(aFileName);

        if (file.isFile() && !file.toPath().toString().contains("juice")) {
            dumpAllLines(file, pw, offSet);
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (!f.toPath().toString().contains("juice")) {
                    dumpAllLines(f, pw, offSet);
                }
            }
        }

    }

    private void dumpAllLines(File file, PrintWriter pw, long offset) throws FileNotFoundException, ParseException {
        Scanner s = new Scanner(file);
        WCDatum temp;
        String tempstr;

        while (s.hasNext()) {
            temp = WCDatum.parseWCDatum(s.nextLine().trim());
            tempstr = "" + ((temp.getReqTime() - offset)/1000) + "\t" + temp.getClientID() + "\t" + temp.getServerID() + "\t" + temp.getObjSize() + "\n";
            pw.append(tempstr);
        }
    }

    public void open(String fName) throws FileNotFoundException {
        fileScan=new Scanner(new File(fName));
    }

    public boolean hasNext() {
        return fileScan.hasNext();
    }

    public List<WCDatum> readNRecords(int limit) {
        LinkedList<WCDatum> wcList = new LinkedList<WCDatum>();
        int counter=0;
        
        while(fileScan.hasNext() && counter++ < limit )
            wcList.add(new WCDatum(fileScan.nextLine().trim()));
        
        return wcList;
    }

}
