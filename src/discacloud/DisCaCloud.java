package discacloud;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.NetworkTopology;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import parsers.RequestDatum;
import parsers.RequestTextReaderInterface;
import parsers.WCTextReader;
import parsers.WSharkTextReader;

public class DisCaCloud {

    private static int vmid = 0;
    private static int clid = 0;
    private static DecimalFormat dft = new DecimalFormat("###,#00.0");

    public static void main(String[] args) throws FileNotFoundException, UnknownHostException, IOException {

        boolean batch = false;
        String fileName = "";

        if (args.length > 0) {
            batch = true;
            int val = (int) (Double.parseDouble(args[1]) * 1000);
            fileName = val + ".txt";
            System.out.println(fileName);
        }

        Log.disable();
        Log.disableFile();
        Log.printLine("Starting DisCaCloud...");

        int[] dcLoads = new int[102];

        try {
            int num_user = 100;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            //CONFIGURATION
            CloudSim.setCacheQuantum(batch ? Integer.parseInt(args[0]) : 1000);
            Log.setIntervalDuration(CloudSim.getCacheQuantum());
            CloudSim.setAggression(batch ? Double.parseDouble(args[1]) : 1);
            int mainDcId;
            int planeSize = 1000;
            boolean geoLocation = true;
            String requestFile = "wSharkLogs/juice1M.txt";
            RequestTextReaderInterface wsReader = new WSharkTextReader();
            int numRecords = batch ? Integer.parseInt(args[2]) : 1000;
            int numRequests = 0;
            int timeOffset = 0;
            double timeDiv = 10;

            HashMap<Integer, String> labelMap = new HashMap<>();
            NetworkTopology.buildNetworkTopology("C:\\atakan.brite");

            HashMap<Integer, Datacenter> dcList = new HashMap<>();
            HashMap<Integer, DatacenterBroker> brList = new HashMap<>();
            HashSet<Integer> dataObjectIds = new HashSet<>();

            for (int i = 0; i < 100; i++) {
                Datacenter dc = createDatacenter("DC_" + i); //labels.get(i)
                dcList.put(dc.getId(), dc);
                NetworkTopology.mapNode(dc.getId(), i);
            }

            for (Datacenter dc : dcList.values()) {
                labelMap.put(dc.getId(), dc.getName());
                String name = dc.getName() + "_BROKER";
                DatacenterBroker br = createBroker(name);
                br.setBindedDC(dc.getId());
                dc.setBindedBR(br.getId());
                brList.put(br.getId(), br);
                NetworkTopology.addLink(dc.getId(), br.getId(), 10.0, 0.1);
            }
            Datacenter.setLabelMap(labelMap);
            mainDcId = NetworkTopology.getMostCentralDc();

            File GeoDatabase = null;
            DatabaseReader reader = null;
            if (geoLocation) {
                GeoDatabase = new File("C:\\GeoLite2-City.mmdb");
                reader = new DatabaseReader.Builder(GeoDatabase).build();
            }
            wsReader.open(requestFile);
            int storageSize = 0;
            for (RequestDatum w : wsReader.readNRecords(numRecords)) {
                Datacenter selectedDC = null;
                if (geoLocation) {
                    InetAddress clientIP = InetAddress.getByName(w.getClientID());

                    CityResponse clientCity;
                    double lat, lon;
                    try {
                        clientCity = reader.city(clientIP);
                        lat = clientCity.getLocation().getLatitude();
                        lon = clientCity.getLocation().getLongitude();
                    } catch (Exception ex) {
                        continue;
                    }

                    double x = (lat + 90) * planeSize / 180;
                    double y = (lon + 180) * planeSize / 360;
                    x -= 258;
                    x *= (1000.0 / 603.0);
                    y -= 57;
                    y *= (1000.0 / 928.0);
                    selectedDC = dcList.get(NetworkTopology.getClosestNodeId(x, y));
                    dcLoads[selectedDC.getId()]++;
                } else {
                    Object[] values = dcList.values().toArray();
                    Object randomValue = values[Math.abs(w.getClientID().hashCode()) % values.length];
                    selectedDC = (Datacenter) randomValue;
                }
                DatacenterBroker selectedBR = brList.get(selectedDC.getBindedBR());
                createLoad(mainDcId, selectedDC, selectedBR, (int) (w.getReqTime() / timeDiv) - timeOffset, Arrays.asList(w.getServerID().hashCode()));
                numRequests++;
                int dataObjectId = w.getServerID().hashCode();
                if (dataObjectIds.add(dataObjectId)) {
                    dcList.get(mainDcId).addDataToMainDC(dataObjectId, (w.getLength()));
                    storageSize += w.getLength();
                }
                //System.out.println("From " + w.getClientID() + " to " + w.getServerID() + " at " + w.getReqTime() + " with size " + w.getLength());
            }

            //System.out.println(Arrays.toString(dcLoads));
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            //Log.enable();
            List<Cloudlet> newList = new ArrayList<>();
            for (DatacenterBroker br : brList.values()) {
                newList.addAll(br.getCloudletReceivedList());
            }

            if (!Log.isDisabled()) {
                Collections.sort(newList);
                printCloudletList(newList);
            }

            if (batch) {
                String text = ("[Quantum, Aggression, MainDC, GeoLocation, Input] = [" + CloudSim.getCacheQuantum() + ", " + CloudSim.getAggression() + ", " + mainDcId + ", " + geoLocation + ", " + requestFile + "]");
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (dft.format(newList.get(newList.size() - 1).getFinishTime()));
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + numRequests;
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + newList.size();
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (dataObjectIds.size());
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (Log.getDataReturnedFromMainDC());
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (Log.getDataReturnedFromCache());
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (Log.getDataFoundInLocalCache());
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (Log.getDataFoundInLocalMainDC());
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (Log.getDataNotFound());
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (dft.format(Log.getStorageCost()));
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (dft.format(Log.getMessageLatency(CloudSimTags.REMOTE_DATA_RETURN)));
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (dft.format(Log.getMessageLatency(CloudSimTags.REMOTE_DATA_NOT_FOUND)));
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (dft.format(newList.get(newList.size() - 1).getFinishTime() * storageSize * CloudSim.storageCosts.get(mainDcId)));
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + (dft.format(Log.getBandwidthCost()));
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
                text = "\n" + ("[Creation, Duplication, Migration, Removal] = [" + Log.getCreation() + ", " + Log.getDuplication() + ", " + Log.getMigration() + ", " + Log.getRemoval() + "]") + "\n----\n";
                Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.APPEND);
            } else {
                Log.printIntervals();
                System.out.println("Configuration: [Quantum, Aggression, MainDC, GeoLocation, Input] = [" + CloudSim.getCacheQuantum() + ", " + CloudSim.getAggression() + ", " + mainDcId + ", " + geoLocation + ", " + requestFile + "]");
                System.out.println("Finish Time: " + dft.format(newList.get(newList.size() - 1).getFinishTime()));
                System.out.println("Number of Requests: " + numRequests);
                System.out.println("Number of Successful Requests: " + newList.size());
                System.out.println("Number of Unique Data Objects: " + dataObjectIds.size());
                System.out.println("Number of Data Objects Received from Main DC: " + Log.getDataReturnedFromMainDC());
                System.out.println("Number of Data Objects Received from a Cache: " + Log.getDataReturnedFromCache());
                System.out.println("Number of Data Objects Found in Local Cache: " + Log.getDataFoundInLocalCache());
                System.out.println("Number of Data Objects Found in Local Main DC: " + Log.getDataFoundInLocalMainDC());
                System.out.println("Number of Data Objects Not Found Initially: " + Log.getDataNotFound());
                System.out.println("Storage Cost: " + dft.format(Log.getStorageCost()));
                System.out.println("Total Latency: " + dft.format(Log.getMessageLatency(CloudSimTags.REMOTE_DATA_RETURN)));
                System.out.println("Total Failure Latency: " + dft.format(Log.getMessageLatency(CloudSimTags.REMOTE_DATA_NOT_FOUND)));
                System.out.println("Main Storage Cost: " + dft.format(newList.get(newList.size() - 1).getFinishTime() * storageSize * CloudSim.storageCosts.get(mainDcId)));
                System.out.println("Bandwidth Cost: " + dft.format(Log.getBandwidthCost()));
                System.out.println("OPERATIONS: [Creation, Duplication, Migration, Removal] = [" + Log.getCreation() + ", " + Log.getDuplication() + ", " + Log.getMigration() + ", " + Log.getRemoval() + "]");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    /**
     * Creates the datacenter.
     *
     * @param name the name
     *
     * @return the datacenter
     */
    private static Datacenter createDatacenter(String name) {

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store
        // our machine
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 1000;

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        // 4. Create Host with its id and list of PEs and add them to the list
        // of machines
        int hostId = 0;
        int ram = 1000 * 1024; // host memory (MB)
        long storage = 100000000; // host storage
        int bw = 1000000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeSharedOverSubscription(peList)
                )
        ); // This is our machine

        // 5. Create a DatacenterCharacteristics object that stores the
        // properties of a data center: architecture, OS, list of
        // Machines, allocation policy: time- or space-shared, time zone
        // and its price (G$/Pe time unit).
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this
        // resource
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
        // devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CloudSim.storageCosts.put(datacenter.getId(), 0.03);  //Amazon S3 per GB
        CloudSim.bandwidthCosts.put(datacenter.getId(), 0.09); //Amazon S3 per GB

        return datacenter;
    }

    // We strongly encourage users to develop their own broker policies, to
    // submit vms and cloudlets according
    // to the specific rules of the simulated scenario
    /**
     * Creates the broker.
     *
     * @return the datacenter broker
     */
    private static DatacenterBroker createBroker(String name) {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    /**
     * Prints the Cloudlet objects.
     *
     * @param list list of Cloudlets
     */
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "\t";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + indent
                + "DC ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start" + indent + "Finish");

        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
            } else {
                Log.print("FAILED");
            }
            Log.printLine(indent + indent + cloudlet.getResourceId()
                    + indent + cloudlet.getVmId()
                    + indent
                    + dft.format(cloudlet.getActualCPUTime()) + indent
                    + dft.format(cloudlet.getExecStartTime())
                    + indent
                    + dft.format(cloudlet.getFinishTime()));

        }
    }

    private static void createLoad(int mainDcId, Datacenter dc, DatacenterBroker br, int start, List<Integer> dataRequests) {
        int mips = 500;
        long size = 10000; // image size (MB)
        int ram = 512; // vm memory (MB)
        long bw = 1000;
        int pesNumber = 1; // number of cpus
        String vmm = "Xen"; // VMM name

        Vm newVm = new Vm(vmid++, br.getId(), start, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
        br.addVm(newVm);

        long length = 500;
        long fileSize = 300;
        long outputSize = 300;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet cloudlet = new Cloudlet(clid++, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
        cloudlet.setUserId(br.getId());
        cloudlet.setVmId(newVm.getId());
        cloudlet.setMainDC(mainDcId);
        for (int dr : dataRequests) {
            cloudlet.addDataRequest(dr);
        }
        br.addCloudlet(cloudlet);
    }

}
