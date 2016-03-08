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
public class WCDatum {

    private long reqTime;
    private int clientID;
    private int length;
    private int serverID;

    public static WCDatum parseWCDatum(String line) throws ParseException {
        WCDatum wc = new WCDatum();
        String[] lineArr = line.split(" ");

        wc.parseReqTime(lineArr[3] + " " + lineArr[4]);
        wc.parseClientID(lineArr[0]);
        wc.parseObjSize(lineArr[9]);
        wc.parseServerID(lineArr[5] + " " + lineArr[6]);

        return wc;
    }

    private WCDatum() {

    }

    public WCDatum(String str) {
        String[] s = str.split("\t");
        reqTime=Long.parseLong(s[0]);
        clientID=Integer.parseInt(s[1]);
        length=Integer.parseInt(s[2]);
        serverID=Integer.parseInt(s[3]);
    }

    public long getReqTime() {
        return reqTime;
    }

    public int getClientID() {
        return clientID;
    }

    public int getObjSize() {
        return length;
    }

    public int getServerID() {
        return serverID;
    }

    private void parseReqTime(String str) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("[dd/MMM/yyyy:hh:mm:ss Z]", Locale.US);
        Date date = formatter.parse(str);
        Calendar time = Calendar.getInstance();
        time.setTime(date);
        reqTime = time.getTimeInMillis();
    }

    private void parseClientID(String str) {
        clientID = Integer.parseInt(str);
    }

    private void parseObjSize(String str) {
        if (!Character.isDigit(str.charAt(0))) {
            length = 0;
        } else {
            length = Integer.parseInt(str);
        }
    }

    @Override
    public String toString() {
        return "WCDatum{" + "reqTime=" + reqTime + ", clientID=" + clientID + ", serverID=" + serverID + ", length=" + length + '}';
    }

    private void parseServerID(String str) {

        serverID = str.hashCode();
    }

}
