/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parsers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author ovatman
 */
public class WSharkDatum {
    private long reqTime;
    private String clientID;
    private String serverID;
    private int length;

    public static WSharkDatum parseWSharkDatum(String line) throws ParseException{
        WSharkDatum wc=new WSharkDatum();
        String[] lineArr=line.split(" ");
        
        wc.parseReqTime(lineArr[1]);
        wc.parseClientID(lineArr[2]);
        wc.parseObjSize(lineArr[3]);
        wc.parseObjID(lineArr[5]);
        
        
        return wc;
    }
    private WSharkDatum() {

    }

    public WSharkDatum(String str) {
        String[] s = str.split("\t");
        reqTime=Long.parseLong(s[0]);
        clientID=s[1];
        serverID=s[2];
        length=Integer.parseInt(s[3]);
    }
    
    public long getReqTime() {
        return reqTime;
    }

    public String getClientID() {
        return clientID;
    }

    public String getServerID() {
        return serverID;
    }

    public int getLength() {
        return length;
    }
    

    private void parseReqTime(String str) throws ParseException {
        reqTime = (int)(Double.parseDouble(str)*1000000);
    }

    private void parseClientID(String str) {
        clientID=str;
    }

    private void parseObjSize(String str) {
       serverID=str;
    }

    private void parseObjID(String str) {
       length=Integer.parseInt(str);
    }

    @Override
    public String toString() {
        return "WSharkDatum{" + "reqTime=" + reqTime + ", clientID=" + clientID + ", serverID=" + serverID + ", length=" + length + '}';
    }

    
    
}
