/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.PrintFile;

/**
 * The Log class used for performing loggin of the simulation process. It
 * provides the ability to substitute the output stream by any OutputStream
 * subclass.
 *
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class Log {

    /**
     * The Constant LINE_SEPARATOR.
     */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
    private static int messageCount;
    private static int notificationCount;
    
    public static void newMessage(boolean isNotification){
        if(isNotification) notificationCount++;
        else messageCount++;
    }
    
    public static double getNotificationPercentage(){
        return (double)(notificationCount) / (double)(messageCount+notificationCount);
    }

    private static int fromCache = 0;
    private static int fromMain = 0;
    private static int fromLocal = 0;
    private static int fromLocalMain = 0;
    private static int fail = 0;

    public static void dataReturnedFromCache() {
        fromCache++;
    }

    public static void dataReturnedFromMainDC() {
        fromMain++;
    }

    public static void dataFoundInLocalCache() {
        fromLocal++;
    }

    public static void dataFoundInLocalMainDC() {
        fromLocalMain++;
    }

    public static void dataNotFound() {
        fail++;
    }

    public static int getDataFoundInLocalCache() {
        return fromLocal;
    }

    public static int getDataFoundInLocalMainDC() {
        return fromLocalMain;
    }

    public static int getDataReturnedFromCache() {
        return fromCache;
    }

    public static int getDataReturnedFromMainDC() {
        return fromMain;
    }

    public static int getDataNotFound() {
        return fail;
    }

    private static int creation = 0;
    private static int removal = 0;
    private static int migration = 0;
    private static int duplication = 0;

    private static double intervalDuration;
    private static int maxInterval = 0;

    //ATAKAN: <intervalNo, count>
    private static final HashMap<Integer, Integer> creations = new HashMap<>();
    private static final HashMap<Integer, Integer> removals = new HashMap<>();
    private static final HashMap<Integer, Integer> migrations = new HashMap<>();
    private static final HashMap<Integer, Integer> duplications = new HashMap<>();

    public static void setIntervalDuration(double intervalDuration) {
        Log.intervalDuration = intervalDuration;
    }

    public static void intervalCount(HashMap<Integer, Integer> hm) {
        int intervalNo = (int) Math.floor(CloudSim.clock() / intervalDuration);
        int val = hm.containsKey(intervalNo) ? hm.get(intervalNo) : 0;
        hm.put(intervalNo, val + 1);
        maxInterval = intervalNo;
    }

    public static void creation() {
        intervalCount(creations);
        creation++;
    }

    public static void removal() {
        intervalCount(removals);
        removal++;
    }

    public static void migration() {
        intervalCount(migrations);
        migration++;
    }

    public static void duplication() {
        intervalCount(duplications);
        duplication++;
    }

    public static void printIntervals() {
        for (int i=0; i<=maxInterval; i++) {
            System.out.println(i + " " + creations.get(i) + " " + removals.get(i) + " " + migrations.get(i) + " " + duplications.get(i));
        }
    }

    public static int getCreation() {
        return creation;
    }

    public static int getRemoval() {
        return removal;
    }

    public static int getMigration() {
        return migration;
    }

    public static int getDuplication() {
        return duplication;
    }

    /**
     * The output.
     */
    private static OutputStream output;

    /**
     * The disable output flag.
     */
    private static boolean disabled;
    private static boolean fileDisabled;

    //ATAKAN: <Cache ID (DC + DataObject), time>
    private static final HashMap<String, Double> caches = new HashMap<>();
    private static double storageCost = 0.0;
    private static double bandwidthCost = 0.0;

    //ATAKAN: <Message Tag, latencies>
    private static final HashMap<Integer, Double> latencies = new HashMap<>();

    /**
     * Prints the message.
     *
     * @param message the message
     */
    public static void print(String message) {
        if (!isDisabled()) {
            try {
                getOutput().write(message.getBytes());
                if (!isFileDisabled()) {
                    PrintFile.AddtoFile(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Prints the message passed as a non-String object.
     *
     * @param message the message
     */
    public static void print(Object message) {
        if (!isDisabled()) {
            print(String.valueOf(message));
        }
    }

    /**
     * Prints the line.
     *
     * @param message the message
     */
    public static void printLine(String message) {
        if (!isDisabled()) {
            print(message + LINE_SEPARATOR);
        }
    }

    /**
     * Prints the empty line.
     */
    public static void printLine() {
        if (!isDisabled()) {
            print(LINE_SEPARATOR);
        }
    }

    /**
     * Prints the line passed as a non-String object.
     *
     * @param message the message
     */
    public static void printLine(Object message) {
        if (!isDisabled()) {
            printLine(String.valueOf(message));
        }
    }

    /**
     * Prints a string formated as in String.format().
     *
     * @param format the format
     * @param args the args
     */
    public static void format(String format, Object... args) {
        if (!isDisabled()) {
            print(String.format(format, args));
        }
    }

    /**
     * Prints a line formated as in String.format().
     *
     * @param format the format
     * @param args the args
     */
    public static void formatLine(String format, Object... args) {
        if (!isDisabled()) {
            printLine(String.format(format, args));
        }
    }

    /**
     * Sets the output.
     *
     * @param _output the new output
     */
    public static void setOutput(OutputStream _output) {
        output = _output;
    }

    /**
     * Gets the output.
     *
     * @return the output
     */
    public static OutputStream getOutput() {
        if (output == null) {
            setOutput(System.out);
        }
        return output;
    }

    /**
     * Sets the disable output flag.
     *
     * @param _disabled the new disabled
     */
    public static void setDisabled(boolean _disabled) {
        disabled = _disabled;
    }

    public static void setFileDisabled(boolean _disabled) {
        fileDisabled = _disabled;
    }

    /**
     * Checks if the output is disabled.
     *
     * @return true, if is disable
     */
    public static boolean isDisabled() {
        return disabled;
    }

    public static boolean isFileDisabled() {
        return fileDisabled;
    }

    /**
     * Disables the output.
     */
    public static void disable() {
        setDisabled(true);
        setFileDisabled(true);
    }

    public static void disableFile() {
        setFileDisabled(true);
    }

    /**
     * Enables the output.
     */
    public static void enable() {
        setDisabled(false);
    }

    //ATAKAN: Log cost
    public static void cacheStart(int DcId, int dataObjectID) {
        String cacheId = DcId + "-" + dataObjectID;
        caches.put(cacheId, CloudSim.clock());
    }

    public static void cacheEnd(int DcId, int dataObjectID, int length) {
        double unitCost = CloudSim.storageCosts.get(DcId);
        String cacheId = DcId + "-" + dataObjectID;
        if (caches.containsKey(cacheId)) {
            double start = caches.remove(cacheId);
            double duration = CloudSim.clock() - start;
            storageCost += unitCost * length * duration;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static double getStorageCost() {
        return storageCost;
    }

    public static void addBandwidthCost(int sourceID, int destinationID, int length) {
        double unitCost = CloudSim.bandwidthCosts.get(sourceID);
        double hopCount = NetworkTopology.getHopCount(sourceID, destinationID);
        bandwidthCost += unitCost * length * hopCount;
    }

    public static double getBandwidthCost() {
        return bandwidthCost;
    }

    //ATAKAN: Log latency
    public static void addLatency(int message, double latency) {
        double val = latency;
        if (latencies.containsKey(message)) {
            val += latencies.get(message);
        }
        latencies.put(message, val);
    }

    public static double getTotalLatency() {
        double totalLatency = 0;
        for (int m : latencies.keySet()) {
            totalLatency += latencies.get(m);
        }
        return totalLatency;
    }

    public static double getMessageLatency(int message) {
        return latencies.getOrDefault(message, 0.0);
    }
}
