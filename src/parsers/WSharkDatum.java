/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parsers;

import java.text.ParseException;

/**
 *
 * @author ovatman
 */
public class WSharkDatum extends RequestDatum {

    public WSharkDatum(String str) {
        super(str);
    }

    public WSharkDatum() {
    }

    public static WSharkDatum parseWSharkDatum(String line) throws ParseException {
        WSharkDatum wc = new WSharkDatum();
        String[] lineArr = line.split(" ");

        wc.parseReqTime(lineArr[1]);
        wc.parseClientID(lineArr[2]);
        wc.parseObjSize(lineArr[3]);
        wc.parseObjID(lineArr[5]);

        return wc;
    }

    private void parseReqTime(String str) throws ParseException {
        reqTime = (int) (Double.parseDouble(str) * 1000000);
    }

    private void parseClientID(String str) {
        clientID = str;
    }

    private void parseObjSize(String str) {
        serverID = str;
    }

    private void parseObjID(String str) {
        length = Integer.parseInt(str);
    }

    @Override
    public String toString() {
        return "WSharkDatum{" + "reqTime=" + reqTime + ", clientID=" + clientID + ", serverID=" + serverID + ", length=" + length + '}';
    }

}
