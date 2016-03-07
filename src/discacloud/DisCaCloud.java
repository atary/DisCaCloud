package discacloud;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
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
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class DisCaCloud {

    private static int vmid = 0;
    private static int clid = 0;
    private static DecimalFormat dft = new DecimalFormat("00.0");

    public static void main(String[] args) {
        //Log.disable();
        Log.disableFile();
        Log.printLine("Starting DisCaCloud...");

        try {
            int num_user = 15;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            //CONFIGURATION
            CloudSim.setCacheQuantum(100);
            CloudSim.setAggression(0.005);
            int dataObjectCount = 2;
            int dataObjectLength = 100;
            int mainDcIndex = 0;

            ArrayList<String> labels = new ArrayList<>(Arrays.asList("GARR", "DFN", "CESNET", "PSNC", "FCCN", "GRNET", "HEANET", "I2CAT", "ICCS", "KTH", "NIIF", "PSNC-2", "RedIRIS", "SWITCH", "NORDUNET"));
            HashMap<Integer, String> labelMap = new HashMap<>();
            NetworkTopology.buildNetworkTopology("C:\\federica.brite");

            ArrayList<Datacenter> dcList = new ArrayList<>();
            ArrayList<DatacenterBroker> brList = new ArrayList<>();

            for (int i = 0; i < 15; i++) {
                Datacenter dc = createDatacenter(labels.get(i));
                dcList.add(dc);
                NetworkTopology.mapNode(dc.getId(), i);
            }

            for (int i = 0; i < dataObjectCount; i++) {
                dcList.get(mainDcIndex).addDataToMainDC(i, dataObjectLength); //GARR is the main DC
            }

            for (Datacenter dc : dcList) {
                labelMap.put(dc.getId(), dc.getName());
                String name = dc.getName() + "_BROKER";
                labels.add(name);
                DatacenterBroker br = createBroker(name);
                br.setBindedDC(dc.getId());
                brList.add(br);
                NetworkTopology.addLink(dc.getId(), br.getId(), 10.0, 0.1);
            }
            Datacenter.setLabelMap(labelMap);

            int mainDcId = dcList.get(mainDcIndex).getId();

            createLoad(mainDcId, dcList.get(6), brList.get(6), 100, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 120, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 180, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 190, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 220, Arrays.asList(1));
            createLoad(mainDcId, dcList.get(6), brList.get(6), 290, Arrays.asList(1));

            // Sixth step: Starts the simulation
            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            //Final step: Print results when simulation is over
            List<Cloudlet> newList = brList.get(6).getCloudletReceivedList();
            printCloudletList(newList);

            Log.printLine("Total Cost: " + (int) Log.getTotalCost());
            Log.printLine("Total Latency: " + dft.format(Log.getMessageLatency(CloudSimTags.REMOTE_DATA_RETURN)));
            Log.printLine("Total Failure Latency: " + dft.format(Log.getMessageLatency(CloudSimTags.REMOTE_DATA_NOT_FOUND)));

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
        int ram = 2048; // host memory (MB)
        long storage = 1000000; // host storage
        int bw = 10000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)
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

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + cloudlet.getResourceId()
                        + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent
                        + dft.format(cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime()));
            }
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

        long length = 4000;
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
