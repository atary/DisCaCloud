package discacloud;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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
        Log.disable();
        Log.disableFile();
        Log.printLine("Starting DisCaCloud...");

        try {
            int num_user = 100;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            //CONFIGURATION
            CloudSim.setCacheQuantum(200);
            CloudSim.setAggression(0.011);
            int mainDcId;
            int planeSize = 1000;
            boolean geoLocation = false;
            String requestFile = "wcLogs/juice880K.txt";
            RequestTextReaderInterface wsReader = new WCTextReader();
            int numRecords = 10000;
            int numRequests = 0;
            int timeOffset = 1785;

            //ArrayList<String> labels = new ArrayList<>(Arrays.asList("GARR", "DFN", "CESNET", "PSNC", "FCCN", "GRNET", "HEANET", "I2CAT", "ICCS", "KTH", "NIIF", "PSNC-2", "RedIRIS", "SWITCH", "NORDUNET"));
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


            /*for (int i = 0; i < dataObjectCount; i++) {
                dcList.get(mainDcId).addDataToMainDC(i, dataObjectLength); //GARR is the main DC
            }*/
            for (Datacenter dc : dcList.values()) {
                labelMap.put(dc.getId(), dc.getName());
                String name = dc.getName() + "_BROKER";
                //labels.add(name);
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
            for (RequestDatum w : wsReader.readNRecords(numRecords)) {
                Datacenter selectedDC = null;
                if (geoLocation) {
                    InetAddress clientIP = InetAddress.getByName(w.getClientID());

                    CityResponse clientCity;

                    try {
                        clientCity = reader.city(clientIP);
                    } catch (GeoIp2Exception ex) {
                        continue;
                    }

                    double lat = clientCity.getLocation().getLatitude();
                    double lon = clientCity.getLocation().getLongitude();

                    double x = (lat + 90) * planeSize / 180;
                    double y = (lon + 180) * planeSize / 360;

                    selectedDC = dcList.get(NetworkTopology.getClosestNodeId(x, y));
                } else {
                    Object[] values = dcList.values().toArray(); //BURADA KALDI
                    Object randomValue = values[Math.abs(w.getClientID().hashCode()) % values.length];
                    selectedDC = (Datacenter) randomValue;
                }
                DatacenterBroker selectedBR = brList.get(selectedDC.getBindedBR());
                createLoad(mainDcId, selectedDC, selectedBR, (int) w.getReqTime() - timeOffset, Arrays.asList(w.getServerID().hashCode()));
                numRequests++;
                int dataObjectId = w.getServerID().hashCode();
                if (dataObjectIds.add(dataObjectId)) {
                    dcList.get(mainDcId).addDataToMainDC(dataObjectId, (w.getLength()));
                }
                //System.out.println("From " + w.getClientID() + " to " + w.getServerID() + " at " + w.getReqTime() + " with size " + w.getLength());
            }

            /*createLoad(mainDcId, dcList.get(6), brList.get(6), 100, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 120, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 180, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 190, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 220, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 290, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(7), brList.get(7), 100, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(7), brList.get(7), 120, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(7), brList.get(7), 180, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(7), brList.get(7), 190, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(7), brList.get(7), 220, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(7), brList.get(7), 290, Arrays.asList(1));*/
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
            System.out.println("Total Cost: " + dft.format(Log.getTotalCost()));
            System.out.println("Total Latency: " + dft.format(Log.getMessageLatency(CloudSimTags.REMOTE_DATA_RETURN)));
            System.out.println("Total Failure Latency: " + dft.format(Log.getMessageLatency(CloudSimTags.REMOTE_DATA_NOT_FOUND)));
            System.out.println("OPERATIONS: [Creation, Duplication, Migration, Removal] = [" + Log.getCreation() + ", " + Log.getDuplication() + ", " + Log.getMigration() + ", " + Log.getRemoval() + "]");

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

        CloudSim.DcCosts.put(datacenter.getId(), 0.01);

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
