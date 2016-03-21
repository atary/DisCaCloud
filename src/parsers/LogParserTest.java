/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;

/**
 *
 * @author ovatman
 */
public class LogParserTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, FileNotFoundException, ParseException {
        //WCTextReader wcReader = new WCTextReader();
        
        //Toplu halde oku ekrana bas
        //for(WCDatum w:wcReader.read("wcLogs"))
        //    System.out.println(w);
        
        //Toplu halde oku juice.txt içine kendi formatıyla bas. juice.txt adı değişmemeli
        //long minTime=Long.parseLong("893971817000");
                
        //PrintWriter pw=new PrintWriter(new FileOutputStream(new File("wcLogs/juice.txt"),false));
        //pw.print("");
        
        //wcReader.dumpAll("C:\\recreate.out",pw,minTime);
        //pw.close();
        
        //juice.txt'den okuyup 6'lı 7'li bas
        //wcReader.open("wcLogs/juice.txt");
        
        //while(wcReader.hasNext())
        //    for(WCDatum w:wcReader.readNRecords((int)(Math.random()*10)))
        //        System.out.println(w);
        
        WSharkTextReader wsReader = new WSharkTextReader();
        //for(WSharkDatum w:wsReader.read("wSharkLogs"))
        //    System.out.println(w);
                
        //Toplu halde oku juice.txt içine kendi formatıyla bas. juice.txt adı değişmemeli       
        PrintWriter pw=new PrintWriter(new FileOutputStream(new File("wSharkLogs/juice.txt"),false));
        pw.print("");
        
        wsReader.dumpAll("wSharkLogs",pw);
        pw.close();
                
        //juice.txt'den okuyup 6'lı 7'li bas
        //wsReader.open("wSharkLogs/juice.txt");
        
        //while(wsReader.hasNext())
        //    for(WSharkDatum w:wsReader.readNRecords((int)(Math.random()*10)))
        //        System.out.println(w);
    }


}
