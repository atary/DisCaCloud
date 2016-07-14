/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 * 
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

/**
 * Datacenter class is a CloudResource whose hostList are virtualized. It deals
 * with processing of VM queries (i.e., handling of VMs) instead of processing
 * Cloudlet-related queries. So, even though an AllocPolicy will be instantiated
 * (in the init() method of the superclass, it will not be used, as processing
 * of cloudlets are handled by the CloudletScheduler and processing of
 * VirtualMachines are handled by the VmAllocationPolicy.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class Datacenter extends SimEntity {

    /**
     * The characteristics.
     */
    private DatacenterCharacteristics characteristics;

    /**
     * The regional cis name.
     */
    private String regionalCisName;

    /**
     * The vm provisioner.
     */
    private VmAllocationPolicy vmAllocationPolicy;

    /**
     * The last process time.
     */
    private double lastProcessTime;

    /**
     * The storage list.
     */
    private List<Storage> storageList;

    /**
     * The vm list.
     */
    private List<? extends Vm> vmList;

    /**
     * The scheduling interval.
     */
    private double schedulingInterval;

    //ATAKAN: <DataObjectID, DatacenterID> Stores known cache locations.
    private HashSetValuedHashMap<Integer, Integer> cacheLocations;

    private boolean mainStorage;

    public void addDataToMainDC(int dataObjectID, int length) {
        mainStorage = true;
        caches.add(new Cache(dataObjectID, length));
        //Log.cacheStart(getId(), dataObjectID);
    }

    //ATAKAN: <DataObjectID> Stores the caches (DataObjectID) that are kept in this datacenter.
    private HashSet<Cache> caches;

    //ATAKAN: <DataObjectID, CloudSimClock> Last usage times of caches for LRU.
    private HashMap<Integer, Double> lru;

    //ATAKAN: <DatacenterID, Latency> latencies to the known non-neigbour datacenters.
    private HashMap<Integer, Double> knownDistances;

    //ATAKAN: <DataObjectID, NeighbourDatacenterID> Log of requests received from neighbours.
    private HashSet<Request> requests;

    private static HashMap<Integer, String> labelMap = null;

    public static void setLabelMap(HashMap<Integer, String> labelMap) {
        Datacenter.labelMap = labelMap;
    }

    private static int cloudletStarted = 0;
    private static int cloudletFinished = 0;

    private boolean firstCheckScheduled;

    private int bindedBR;

    /**
     * Allocates a new PowerDatacenter object.
     *
     * @param name the name to be associated with this entity (as required by
     * Sim_entity class from simjava package)
     * @param characteristics an object of DatacenterCharacteristics
     * @param storageList a LinkedList of storage elements, for data simulation
     * @param vmAllocationPolicy the vmAllocationPolicy
     * @throws Exception This happens when one of the following scenarios occur:
     * <ul>
     * <li>creating this entity before initializing CloudSim package
     * <li>this entity name is <tt>null</tt> or empty
     * <li>this entity has <tt>zero</tt> number of PEs (Processing Elements).
     * <br>
     * No PEs mean the Cloudlets can't be processed. A CloudResource must
     * contain one or more Machines. A Machine must contain one or more PEs.
     * </ul>
     * @pre name != null
     * @pre resource != null
     * @post $none
     */
    public Datacenter(
            String name,
            DatacenterCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval) throws Exception {
        super(name);

        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);

        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }

        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }

        // stores id of this class
        getCharacteristics().setId(super.getId());

        cacheLocations = new HashSetValuedHashMap<>();
        caches = new HashSet<>();
        lru = new HashMap<>();
        knownDistances = new HashMap<>();
        requests = new HashSet<>();
        mainStorage = false;
        firstCheckScheduled = false;
    }

    /**
     * Overrides this method when making a new and different type of resource.
     * <br>
     * <b>NOTE:</b> You do not need to override {@link #body()} method, if you
     * use this method.
     *
     * @pre $none
     * @post $none
     */
    protected void registerOtherEntity() {
        // empty. This should be override by a child class
    }

    /**
     * Processes events or services that are available for this PowerDatacenter.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        int srcId = -1;
        switch (ev.getTag()) {
            // Resource characteristics inquiry
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), getCharacteristics());
                // ATAKAN: Schedule the first cache check
                if (!firstCheckScheduled && !CloudSim.isCacheEnabled()) {
                    firstCheckScheduled = true;
                    Log.printLine(CloudSim.clock() + ": " + getName() + ": CHECK_DEMAND_FOR_CACHES is scheduled");
                    send(getId(), CloudSim.getCacheQuantum(), CloudSimTags.CHECK_DEMAND_FOR_CACHES);
                }
                break;

            case CloudSimTags.CHECK_DEMAND_FOR_CACHES:
                checkCacheConditions();
                break;

            // Resource dynamic info inquiry
            case CloudSimTags.RESOURCE_DYNAMICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), 0);
                break;

            case CloudSimTags.RESOURCE_NUM_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int numPE = getCharacteristics().getNumberOfPes();
                sendNow(srcId, ev.getTag(), numPE);
                break;

            case CloudSimTags.RESOURCE_NUM_FREE_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int freePesNumber = getCharacteristics().getNumberOfFreePes();
                sendNow(srcId, ev.getTag(), freePesNumber);
                break;

            // New Cloudlet arrives
            case CloudSimTags.CLOUDLET_SUBMIT:
                cloudletStarted++;
                //if(cloudletStarted%1000==0) System.out.println("Cloudlet "+cloudletStarted + " started");
                processCloudletSubmit(ev, false);
                break;

            // New Cloudlet arrives, but the sender asks for an ack
            case CloudSimTags.CLOUDLET_SUBMIT_ACK:
                processCloudletSubmit(ev, true);
                break;

            // Cancels a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_CANCEL:
                processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
                break;

            // Pauses a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_PAUSE:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
                break;

            // Pauses a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case CloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
                break;

            // Resumes a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_RESUME:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
                break;

            // Resumes a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case CloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case CloudSimTags.CLOUDLET_MOVE:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case CloudSimTags.CLOUDLET_MOVE_ACK:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
                break;

            // Checks the status of a Cloudlet
            case CloudSimTags.CLOUDLET_STATUS:
                processCloudletStatus(ev);
                break;

            // Ping packet
            case CloudSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                break;

            case CloudSimTags.VM_CREATE:
                processVmCreate(ev, false);
                break;

            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev, true);
                break;

            case CloudSimTags.VM_DESTROY:
                processVmDestroy(ev, false);
                break;

            case CloudSimTags.VM_DESTROY_ACK:
                processVmDestroy(ev, true);
                break;

            case CloudSimTags.VM_MIGRATE:
                processVmMigrate(ev, false);
                break;

            case CloudSimTags.VM_MIGRATE_ACK:
                processVmMigrate(ev, true);
                break;

            case CloudSimTags.VM_DATA_ADD:
                processDataAdd(ev, false);
                break;

            case CloudSimTags.VM_DATA_ADD_ACK:
                processDataAdd(ev, true);
                break;

            case CloudSimTags.VM_DATA_DEL:
                processDataDelete(ev, false);
                break;

            case CloudSimTags.VM_DATA_DEL_ACK:
                processDataDelete(ev, true);
                break;

            case CloudSimTags.VM_DATACENTER_EVENT:
                updateCloudletProcessing();
                checkCloudletCompletion();
                break;

            case CloudSimTags.REMOTE_DATA_REQUEST:
                processDataRequest(ev);
                break;

            case CloudSimTags.REMOTE_DATA_RETURN:
                processDataReturn(ev);
                break;

            case CloudSimTags.REMOTE_DATA_NOT_FOUND:
                processDataNotFound(ev);
                break;

            case CloudSimTags.CREATE_CACHE:
                processCacheCreation(ev);
                break;

            case CloudSimTags.REMOVE_CACHE:
                processCacheRemoval(ev);
                break;

            case CloudSimTags.ADD_CACHE_LOCATION:
                knownDistances.put(ev.getSource(), CloudSim.clock() - ev.creationTime());
                if (ev.getSource() != getId()) {
                    cacheLocations.put((int) ev.getData(), ev.getSource());
                }
                break;

            case CloudSimTags.REMOVE_CACHE_LOCATION:
                knownDistances.put(ev.getSource(), CloudSim.clock() - ev.creationTime());
                cacheLocations.removeMapping((int) ev.getData(), ev.getSource());
                break;

            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }

    /**
     * Process data del.
     *
     * @param ev the ev
     * @param ack the ack
     */
    protected void processDataDelete(SimEvent ev, boolean ack) {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.getData();
        if (data == null) {
            return;
        }

        String filename = (String) data[0];
        int req_source = ((Integer) data[1]).intValue();
        int tag = -1;

        // check if this file can be deleted (do not delete is right now)
        int msg = deleteFileFromStorage(filename);
        if (msg == DataCloudTags.FILE_DELETE_SUCCESSFUL) {
            tag = DataCloudTags.CTLG_DELETE_MASTER;
        } else { // if an error occured, notify user
            tag = DataCloudTags.FILE_DELETE_MASTER_RESULT;
        }

        if (ack) {
            // send back to sender
            Object pack[] = new Object[2];
            pack[0] = filename;
            pack[1] = Integer.valueOf(msg);

            sendNow(req_source, tag, pack);
        }
    }

    /**
     * Process data add.
     *
     * @param ev the ev
     * @param ack the ack
     */
    protected void processDataAdd(SimEvent ev, boolean ack) {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.getData();
        if (pack == null) {
            return;
        }

        File file = (File) pack[0]; // get the file
        file.setMasterCopy(true); // set the file into a master copy
        int sentFrom = ((Integer) pack[1]).intValue(); // get sender ID

        /**
         * ****
         * // DEBUG Log.printLine(super.get_name() + ".addMasterFile(): " +
         * file.getName() + " from " + CloudSim.getEntityName(sentFrom)); *****
         */
        Object[] data = new Object[3];
        data[0] = file.getName();

        int msg = addFile(file); // add the file

        if (ack) {
            data[1] = Integer.valueOf(-1); // no sender id
            data[2] = Integer.valueOf(msg); // the result of adding a master file
            sendNow(sentFrom, DataCloudTags.FILE_ADD_MASTER_RESULT, data);
        }
    }

    /**
     * Processes a ping request.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processPingRequest(SimEvent ev) {
        InfoPacket pkt = (InfoPacket) ev.getData();
        pkt.setTag(CloudSimTags.INFOPKT_RETURN);
        pkt.setDestId(pkt.getSrcId());

        // sends back to the sender
        sendNow(pkt.getSrcId(), CloudSimTags.INFOPKT_RETURN, pkt);
    }

    /**
     * Process the event for an User/Broker who wants to know the status of a
     * Cloudlet. This PowerDatacenter will then send the status back to the
     * User/Broker.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processCloudletStatus(SimEvent ev) {
        int cloudletId = 0;
        int userId = 0;
        int vmId = 0;
        int status = -1;

        try {
            // if a sender using cloudletXXX() methods
            int data[] = (int[]) ev.getData();
            cloudletId = data[0];
            userId = data[1];
            vmId = data[2];

            status = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId, userId).getCloudletScheduler()
                    .getCloudletStatus(cloudletId);
        } // if a sender using normal send() methods
        catch (ClassCastException c) {
            try {
                Cloudlet cl = (Cloudlet) ev.getData();
                cloudletId = cl.getCloudletId();
                userId = cl.getUserId();

                status = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId, userId)
                        .getCloudletScheduler().getCloudletStatus(cloudletId);
            } catch (Exception e) {
                Log.printLine(getName() + ": Error in processing CloudSimTags.CLOUDLET_STATUS");
                Log.printLine(e.getMessage());
                return;
            }
        } catch (Exception e) {
            Log.printLine(getName() + ": Error in processing CloudSimTags.CLOUDLET_STATUS");
            Log.printLine(e.getMessage());
            return;
        }

        int[] array = new int[3];
        array[0] = getId();
        array[1] = cloudletId;
        array[2] = status;

        int tag = CloudSimTags.CLOUDLET_STATUS;
        sendNow(userId, tag, array);
    }

    /**
     * Here all the method related to VM requests will be received and forwarded
     * to the related method.
     *
     * @param ev the received event
     * @pre $none
     * @post $none
     */
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine(getName() + ".processOtherEvent(): Error - an event is null.");
        }
    }

    /**
     * Process the event for an User/Broker who wants to create a VM in this
     * PowerDatacenter. This PowerDatacenter will then send the status back to
     * the User/Broker.
     *
     * @param ev a Sim_event object
     * @param ack the ack
     * @pre ev != null
     * @post $none
     */
    protected void processVmCreate(SimEvent ev, boolean ack) {
        Vm vm = (Vm) ev.getData();

        boolean result = getVmAllocationPolicy().allocateHostForVm(vm);

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = vm.getId();

            if (result) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            send(vm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
        }

        if (result) {
            getVmList().add(vm);

            if (vm.isBeingInstantiated()) {
                vm.setBeingInstantiated(false);
            }

            vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler()
                    .getAllocatedMipsForVm(vm));
        }

    }

    /**
     * Process the event for an User/Broker who wants to destroy a VM previously
     * created in this PowerDatacenter. This PowerDatacenter may send, upon
     * request, the status back to the User/Broker.
     *
     * @param ev a Sim_event object
     * @param ack the ack
     * @pre ev != null
     * @post $none
     */
    protected void processVmDestroy(SimEvent ev, boolean ack) {
        Vm vm = (Vm) ev.getData();
        getVmAllocationPolicy().deallocateHostForVm(vm);

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = vm.getId();
            data[2] = CloudSimTags.TRUE;

            sendNow(vm.getUserId(), CloudSimTags.VM_DESTROY_ACK, data);
        }

        getVmList().remove(vm);
    }

    /**
     * Process the event for an User/Broker who wants to migrate a VM. This
     * PowerDatacenter will then send the status back to the User/Broker.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processVmMigrate(SimEvent ev, boolean ack) {
        Object tmp = ev.getData();
        if (!(tmp instanceof Map<?, ?>)) {
            throw new ClassCastException("The data object must be Map<String, Object>");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> migrate = (HashMap<String, Object>) tmp;

        Vm vm = (Vm) migrate.get("vm");
        Host host = (Host) migrate.get("host");

        getVmAllocationPolicy().deallocateHostForVm(vm);
        host.removeMigratingInVm(vm);
        boolean result = getVmAllocationPolicy().allocateHostForVm(vm, host);
        if (!result) {
            Log.printLine("[Datacenter.processVmMigrate] VM allocation to the destination host failed");
            System.exit(0);
        }

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = vm.getId();

            if (result) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            sendNow(ev.getSource(), CloudSimTags.VM_CREATE_ACK, data);
        }

        Log.formatLine(
                "%.2f: Migration of VM #%d to Host #%d is completed",
                CloudSim.clock(),
                vm.getId(),
                host.getId());
        vm.setInMigration(false);
    }

    /**
     * Processes a Cloudlet based on the event type.
     *
     * @param ev a Sim_event object
     * @param type event type
     * @pre ev != null
     * @pre type > 0
     * @post $none
     */
    protected void processCloudlet(SimEvent ev, int type) {
        int cloudletId = 0;
        int userId = 0;
        int vmId = 0;

        try { // if the sender using cloudletXXX() methods
            int data[] = (int[]) ev.getData();
            cloudletId = data[0];
            userId = data[1];
            vmId = data[2];
        } // if the sender using normal send() methods
        catch (ClassCastException c) {
            try {
                Cloudlet cl = (Cloudlet) ev.getData();
                cloudletId = cl.getCloudletId();
                userId = cl.getUserId();
                vmId = cl.getVmId();
            } catch (Exception e) {
                Log.printLine(super.getName() + ": Error in processing Cloudlet");
                Log.printLine(e.getMessage());
                return;
            }
        } catch (Exception e) {
            Log.printLine(super.getName() + ": Error in processing a Cloudlet.");
            Log.printLine(e.getMessage());
            return;
        }

        // begins executing ....
        switch (type) {
            case CloudSimTags.CLOUDLET_CANCEL:
                processCloudletCancel(cloudletId, userId, vmId);
                break;

            case CloudSimTags.CLOUDLET_PAUSE:
                processCloudletPause(cloudletId, userId, vmId, false);
                break;

            case CloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudletPause(cloudletId, userId, vmId, true);
                break;

            case CloudSimTags.CLOUDLET_RESUME:
                processCloudletResume(cloudletId, userId, vmId, false);
                break;

            case CloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudletResume(cloudletId, userId, vmId, true);
                break;
            default:
                break;
        }

    }

    /**
     * Process the event for an User/Broker who wants to move a Cloudlet.
     *
     * @param receivedData information about the migration
     * @param type event tag
     * @pre receivedData != null
     * @pre type > 0
     * @post $none
     */
    protected void processCloudletMove(int[] receivedData, int type) {
        updateCloudletProcessing();

        int[] array = receivedData;
        int cloudletId = array[0];
        int userId = array[1];
        int vmId = array[2];
        int vmDestId = array[3];
        int destId = array[4];

        // get the cloudlet
        Cloudlet cl = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId, userId)
                .getCloudletScheduler().cloudletCancel(cloudletId);

        boolean failed = false;
        if (cl == null) {// cloudlet doesn't exist
            failed = true;
        } else {
            // has the cloudlet already finished?
            if (cl.getCloudletStatus() == Cloudlet.SUCCESS) {// if yes, send it back to user
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cloudletId;
                data[2] = 0;
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_SUBMIT_ACK, data);
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
            }

            // prepare cloudlet for migration
            cl.setVmId(vmDestId);

            // the cloudlet will migrate from one vm to another does the destination VM exist?
            if (destId == getId()) {
                Vm vm = getVmAllocationPolicy().getHost(vmDestId, userId).getVm(vmDestId, userId);
                if (vm == null) {
                    failed = true;
                } else {
                    // time to transfer the files
                    double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
                    vm.getCloudletScheduler().cloudletSubmit(cl, fileTransferTime);
                }
            } else {// the cloudlet will migrate from one resource to another
                int tag = ((type == CloudSimTags.CLOUDLET_MOVE_ACK) ? CloudSimTags.CLOUDLET_SUBMIT_ACK
                        : CloudSimTags.CLOUDLET_SUBMIT);
                sendNow(destId, tag, cl);
            }
        }

        if (type == CloudSimTags.CLOUDLET_MOVE_ACK) {// send ACK if requested
            int[] data = new int[3];
            data[0] = getId();
            data[1] = cloudletId;
            if (failed) {
                data[2] = 0;
            } else {
                data[2] = 1;
            }
            sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_SUBMIT_ACK, data);
        }
    }

    /**
     * Processes a Cloudlet submission.
     *
     * @param ev a SimEvent object
     * @param ack an acknowledgement
     * @pre ev != null
     * @post $none
     */
    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
        updateCloudletProcessing();

        try {
            // gets the Cloudlet object
            Cloudlet cl = (Cloudlet) ev.getData();

            // checks whether this Cloudlet has finished or not
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printLine(getName() + ": Warning - Cloudlet #" + cl.getCloudletId() + " owned by " + name
                        + " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();

                // NOTE: If a Cloudlet has finished, then it won't be processed.
                // So, if ack is required, this method sends back a result.
                // If ack is not required, this method don't send back a result.
                // Hence, this might cause CloudSim to be hanged since waiting
                // for this Cloudlet back.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = cl.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                    sendNow(cl.getUserId(), tag, data);
                }

                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

                return;
            }

            // process this Cloudlet to this CloudResource
            cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics()
                    .getCostPerBw());

            int userId = cl.getUserId();
            int vmId = cl.getVmId();

            // ATAKAN: add main storage ID to known caches.
            // time to transfer the files
            double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

            Host host = getVmAllocationPolicy().getHost(vmId, userId);
            Vm vm = host.getVm(vmId, userId);
            CloudletScheduler scheduler = vm.getCloudletScheduler();
            double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);
            // if this cloudlet is in the exec queue
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                //ATAKAN: Pause cloudlet and request data 
                ArrayList<Integer> requiredData = cl.getRequiredData();
                boolean requested = false;
                for (int dataObjectID : requiredData) {
                    boolean found = false;
                    for (Cache c : caches) {
                        if (c.dataObjectID == dataObjectID) {
                            if (c.isLocalCache() && !cl.getCoords().equals(c.getCloudlet().getCoords())) {
                                continue;
                            }
                            cl.addDataReceive(dataObjectID);
                            found = true;
                            Log.printLine(CloudSim.clock() + ": " + getName() + ": Data object #" + c.dataObjectID + " is found locally ");
                            if (mainStorage) {
                                Log.dataFoundInLocalMainDC();
                            } else {
                                Log.dataFoundInLocalCache();
                            }
                            break;
                        }
                    }
                    if (!found) {
                        requested = true;
                        if (getId() != cl.getMainDc()) {
                            cacheLocations.put(dataObjectID, cl.getMainDc());
                        }
                        sendDataRequest(cl.getCloudletId(), cl.getUserId(), cl.getVmId(), dataObjectID);
                    }
                }
                if (requested) {
                    processCloudletPause(cl.getCloudletId(), userId, vmId, false);
                } else {
                    estimatedFinishTime += fileTransferTime;
                    send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT); //Why not schedule?
                }
            }

            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cl.getCloudletId();
                data[2] = CloudSimTags.TRUE;

                // unique tag = operation tag
                int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                sendNow(cl.getUserId(), tag, data);
            }
        } catch (ClassCastException c) {
            Log.printLine(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
            c.printStackTrace();
        } catch (Exception e) {
            Log.printLine(getName() + ".processCloudletSubmit(): " + "Exception error.");
            e.printStackTrace();
        }

        checkCloudletCompletion();
    }

    private void sendDataRequest(int cloudletId, int userId, int vmId, int dataObjectID) {
        int[] data = new int[5];
        data[0] = getId();
        data[1] = cloudletId;
        data[2] = userId;
        data[3] = vmId;
        data[4] = dataObjectID;
        //dataSourceId = dataSourceId < 0 ? storageId : dataSourceId;
        sendNow(getDataCacheId(dataObjectID), CloudSimTags.REMOTE_DATA_REQUEST, data);
    }

    // ATAKAN: Answer if data is here.
    private void processDataRequest(SimEvent ev) {
        knownDistances.put(ev.getSource(), CloudSim.clock() - ev.creationTime());

        int[] data = (int[]) ev.getData();

        boolean found = false; //Check if cache is actually here
        int length = 0;
        if (!found) {
            for (Cache c : caches) {
                if (c.dataObjectID == data[4]) {
                    found = true;
                    length = c.length;
                    break;
                }
            }
        }
        if (found) {
            int[] updatedData = new int[data.length + 1];
            for (int i = 0; i < data.length; i++) {
                updatedData[i] = data[i];
            }
            updatedData[updatedData.length - 1] = length;
            Log.addLatency(CloudSimTags.REMOTE_DATA_RETURN, CloudSim.clock() - ev.creationTime());
            Log.addBandwidthCost(getId(), data[0], length);
            send(data[0], length / NetworkTopology.getBw(), CloudSimTags.REMOTE_DATA_RETURN, updatedData);
            requests.add(new Request(ev.creationTime(), data[0], getId(), data[4], length));
            if (mainStorage) {
                Log.dataReturnedFromMainDC();
            } else {
                Log.dataReturnedFromCache();
            }
        } else {
            Log.addLatency(CloudSimTags.REMOTE_DATA_NOT_FOUND, CloudSim.clock() - ev.creationTime());
            Log.dataNotFound();
            sendNow(data[0], CloudSimTags.REMOTE_DATA_NOT_FOUND, data);
        }
    }

    // ATAKAN: Resume the cloudlet after all data are received.
    private void processDataReturn(SimEvent ev) {
        knownDistances.put(ev.getSource(), CloudSim.clock() - ev.creationTime());
        int[] data = (int[]) ev.getData();
        //System.out.println(getId() + ": " + data[4] + " is received.");
        Cloudlet cl = getVmAllocationPolicy().getHost(data[3], data[2]).getVm(data[3], data[2]).getCloudletScheduler().getCloudlet(data[1]);
        cl.addDataReceive(data[4]);
        if (cl.allRequiredDataAreReceived()) {
            //System.out.println(getId() + ": " + "All received.");
            processCloudletResume(data[1], data[2], data[3], false);
        }
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Data object #" + data[4] + " is received from " + labelMap.get(ev.getSource()));

        if (CloudSim.isCacheEnabled()) {
            int id = data[4];
            int length = data[5];
            if (caches.add(new Cache(id, length, cl))) {
                Log.cacheStart(getId(), id);
                Log.creation();
            }
            lru.put(id, CloudSim.clock());
            while (caches.size() > CloudSim.getCacheLength()) {
                double lruTime = Double.MAX_VALUE;
                Cache lruCache = null;
                for (Cache c : caches) {
                    double time = lru.get(c.getDataObjectID());
                    if (time < lruTime) {
                        lruTime = time;
                        lruCache = c;
                    }
                }
                if (null == lruCache) {
                    throw new RuntimeException("LRU cache is not found");
                } else if (caches.remove(lruCache)) {
                    Log.cacheEnd(getId(), lruCache.getDataObjectID(), length);
                    Log.removal();
                    lru.remove(lruCache.getDataObjectID());
                } else {
                    throw new RuntimeException("LRU cache is already removed");
                }
            }
        }
    }

    private void processDataNotFound(SimEvent ev) {
        knownDistances.put(ev.getSource(), CloudSim.clock() - ev.creationTime());
        int[] data = (int[]) ev.getData();
        int source = ev.getSource();
        cacheLocations.removeMapping(data[4], source);
        sendDataRequest(data[1], data[2], data[3], data[4]);
    }

    private int getDataCacheId(int dataObjectID) {
        double min = Double.MAX_VALUE;
        int minId = -1;
        Set<Integer> locs = cacheLocations.get(dataObjectID);
        if (cacheLocations.containsKey(dataObjectID)) {
            for (int loc : locs) {
                if (knownDistances.containsKey(loc) && knownDistances.get(loc) < min) {
                    min = knownDistances.get(loc);
                    minId = loc;
                }
            }
            if (minId > 0) {
                return minId;
            }
        }
        return locs.iterator().next();
    }

    // ATAKAN: check for cache operation conditions and initiate the selected operation.
    private void checkCacheConditions() {
        if (!caches.isEmpty()) {
            Log.printLine(CloudSim.clock() + ": _____" + getName());
            ArrayList<Integer> neighbours = NetworkTopology.getNeighbours(getId());
            if (mainStorage) {
                for (Cache c : caches) {
                    addRecentRequestors(c);
                    for (int n : neighbours) {
                        double neighbourCost = CloudSim.storageCosts.get(n);
                        if (getDemand(c.dataObjectID, n, 0) > neighbourCost) { //Create Decision
                            send(n, c.length / NetworkTopology.getBw(), CloudSimTags.CREATE_CACHE, c);
                            Log.printLine(CloudSim.clock() + ": +" + getName() + ": Create a new cache for data object #" + c.getDataObjectID() + " in " + labelMap.get(n));
                            Log.creation();
                        }
                    }
                }
            } else {
                for (Cache c : caches) {
                    addRecentRequestors(c);
                    //boolean actionTaken = false;
                    double localCost = CloudSim.storageCosts.get(getId());
                    double allNeighboursDemand = 0;
                    for (int n : neighbours) {
                        double neighbourCost = CloudSim.storageCosts.get(n);

                        double neighbourLatency = NetworkTopology.getDelay(getId(), n);
                        double neighbourDemand = getDemand(c.dataObjectID, n, 0);
                        allNeighboursDemand += neighbourDemand;
                        if (neighbourDemand == 0) {
                            continue;
                        }
                        double neighbourDecreasedDemand = getDemand(c.dataObjectID, n, -neighbourLatency);
                        double otherNeighboursDemand = 0;
                        double otherNeighboursIncreasedDemand = 0;
                        for (int an : neighbours) {
                            if (an != n) {
                                otherNeighboursDemand += getDemand(c.dataObjectID, an, 0);
                                otherNeighboursIncreasedDemand += getDemand(c.dataObjectID, an, neighbourLatency);
                            }
                        }

                        double transferDelay = c.length / NetworkTopology.getBw();
                        if (neighbourDemand > neighbourCost && otherNeighboursDemand > localCost) { //Duplicate Decision
                            Log.printLine(CloudSim.clock() + ": +" + getName() + ": Duplicate cache for data object #" + c.getDataObjectID() + " to " + labelMap.get(n));
                            send(n, transferDelay, CloudSimTags.CREATE_CACHE, c);
                            Log.duplication();
                            break;
                        }
                        if (allNeighboursDemand - (otherNeighboursIncreasedDemand + neighbourDecreasedDemand) > neighbourCost - localCost) { //Migrate Decision
                            Log.printLine(CloudSim.clock() + ": +-" + getName() + ": Migrate cache for data object #" + c.getDataObjectID() + " to " + labelMap.get(n));
                            schedule(getId(), NetworkTopology.getDelay(getId(), n) + transferDelay, CloudSimTags.REMOVE_CACHE, c); //Cache will stay here until create message is delivered.
                            send(n, transferDelay, CloudSimTags.CREATE_CACHE, c);
                            Log.migration();
                            break;
                        }
                    }
                    if (allNeighboursDemand < localCost) { //Remove Decision
                        Log.printLine(CloudSim.clock() + ": -" + getName() + ": Remove cache for data object #" + c.getDataObjectID());
                        scheduleNow(getId(), CloudSimTags.REMOVE_CACHE, c);
                        Log.removal();
                    }
                }
            }
        }
        // Clear log for the current interval and schedule the next check
        requests.clear();
        send(getId(), CloudSim.getCacheQuantum(), CloudSimTags.CHECK_DEMAND_FOR_CACHES);
    }

    private void addRecentRequestors(Cache c) {
        c.recentRequestors.clear();
        for (Request r : requests) {
            if (r.dataObjectID == c.dataObjectID) {
                c.addRequestor(r.originalSource);
            }
        }
    }

    private double getDemand(int dataObjectId, int neighbourId, double latencyOffset) {
        double demand = 0;
        for (Request r : requests) {
            if (r.dataObjectID == dataObjectId && r.neighbourSource == neighbourId) {
                demand += (r.latency + latencyOffset);
            }
        }
        return demand * CloudSim.getAggression();
    }

    //ATAKAN: Create a new cache in this location
    private void processCacheCreation(SimEvent ev) {
        knownDistances.put(ev.getSource(), CloudSim.clock() - ev.creationTime());
        Cache c = (Cache) ev.getData();

        if (caches.add(c)) {
            Log.printLine(CloudSim.clock() + ": +" + getName() + ": Cache for data object #" + c.getDataObjectID() + " is created");
            Log.cacheStart(getId(), c.dataObjectID);
            //Notify other DCs for the new cache
            notifyOtherDCs(CloudSimTags.ADD_CACHE_LOCATION, c);
        }
        //TODO: what if capacity is not enough
    }

    //ATAKAN: Remove a cache from this location
    private void processCacheRemoval(SimEvent ev) {
        Cache c = (Cache) ev.getData();
        if (caches.remove(c)) {
            Log.cacheEnd(getId(), c.dataObjectID, c.length);
            Log.printLine(CloudSim.clock() + ": -" + getName() + ": Cache for data object #" + c.getDataObjectID() + " is removed");
        }

        //Notify other DCs for the removal
        notifyOtherDCs(CloudSimTags.REMOVE_CACHE_LOCATION, c);
    }

    private void notifyOtherDCs(int message, Cache c) {
        int dataObjectID = c.getDataObjectID();
        HashSet<Integer> dcIds = c.getRecentRequestors();
        dcIds.addAll(NetworkTopology.getNeighbours(getId()));
        
        for (int i : dcIds) {
            sendNow(i, message, dataObjectID);
        }
    }

    /**
     * Predict file transfer time.
     *
     * @param requiredFiles the required files
     * @return the double
     */
    protected double predictFileTransferTime(List<String> requiredFiles) {
        double time = 0.0;

        Iterator<String> iter = requiredFiles.iterator();
        while (iter.hasNext()) {
            String fileName = iter.next();
            for (int i = 0; i < getStorageList().size(); i++) {
                Storage tempStorage = getStorageList().get(i);
                File tempFile = tempStorage.getFile(fileName);
                if (tempFile != null) {
                    time += tempFile.getSize() / tempStorage.getMaxTransferRate();
                    break;
                }
            }
        }
        return time;
    }

    /**
     * Processes a Cloudlet resume request.
     *
     * @param cloudletId resuming cloudlet ID
     * @param userId ID of the cloudlet's owner
     * @param ack $true if an ack is requested after operation
     * @param vmId the vm id
     * @pre $none
     * @post $none
     */
    protected void processCloudletResume(int cloudletId, int userId, int vmId, boolean ack) {
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet #" + cloudletId + " is resumed");
        //ATAKAN: Always update paused cloudlets first.
        updateCloudletProcessing();

        double eventTimeToFinish = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId, userId)
                .getCloudletScheduler().cloudletResume(cloudletId);

        boolean status = false;
        if (eventTimeToFinish > 0.0) { // if this cloudlet is in the exec queue
            status = true;
            //ATAKAN: This check is not necessary return value is time to finish. 
            //if (eventTimeToFinish > CloudSim.clock()) {
            schedule(getId(), eventTimeToFinish, CloudSimTags.VM_DATACENTER_EVENT);
            //}
        }

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = cloudletId;
            if (status) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            sendNow(userId, CloudSimTags.CLOUDLET_RESUME_ACK, data);
        }
    }

    /**
     * Processes a Cloudlet pause request.
     *
     * @param cloudletId resuming cloudlet ID
     * @param userId ID of the cloudlet's owner
     * @param ack $true if an ack is requested after operation
     * @param vmId the vm id
     * @pre $none
     * @post $none
     */
    protected void processCloudletPause(int cloudletId, int userId, int vmId, boolean ack) {
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet #" + cloudletId + " is paused");
        boolean status = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId, userId)
                .getCloudletScheduler().cloudletPause(cloudletId);

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = cloudletId;
            if (status) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            sendNow(userId, CloudSimTags.CLOUDLET_PAUSE_ACK, data);
        }
    }

    /**
     * Processes a Cloudlet cancel request.
     *
     * @param cloudletId resuming cloudlet ID
     * @param userId ID of the cloudlet's owner
     * @param vmId the vm id
     * @pre $none
     * @post $none
     */
    protected void processCloudletCancel(int cloudletId, int userId, int vmId) {
        Cloudlet cl = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId, userId).getCloudletScheduler().cloudletCancel(cloudletId);
        sendNow(userId, CloudSimTags.CLOUDLET_CANCEL, cl);
    }

    /**
     * Updates processing of each cloudlet running in this PowerDatacenter. It
     * is necessary because Hosts and VirtualMachines are simple objects, not
     * entities. So, they don't receive events and updating cloudlets inside
     * them must be called from the outside.
     *
     * @pre $none
     * @post $none
     */
    protected void updateCloudletProcessing() {
        // if some time passed since last processing
        // R: for term is to allow loop at simulation start. Otherwise, one initial
        // simulation step is skipped and schedulers are not properly initialized
        if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + CloudSim.getMinTimeBetweenEvents()) {
            List<? extends Host> list = getVmAllocationPolicy().getHostList();
            double smallerTime = Double.MAX_VALUE;
            // for each host...
            for (int i = 0; i < list.size(); i++) {
                Host host = list.get(i);
                // inform VMs to update processing
                double time = host.updateVmsProcessing(CloudSim.clock());
                // what time do we expect that the next cloudlet will finish?
                if (time < smallerTime) {
                    smallerTime = time;
                }
            }
            // gurantees a minimal interval before scheduling the event
            if (smallerTime < CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01) {
                smallerTime = CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01;
            }
            if (smallerTime != Double.MAX_VALUE) {
                schedule(getId(), (smallerTime - CloudSim.clock()), CloudSimTags.VM_DATACENTER_EVENT);
            }
            setLastProcessTime(CloudSim.clock());
        }
    }

    /**
     * Verifies if some cloudlet inside this PowerDatacenter already finished.
     * If yes, send it to the User/Broker
     *
     * @pre $none
     * @post $none
     */
    protected void checkCloudletCompletion() {
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (int i = 0; i < list.size(); i++) {
            Host host = list.get(i);
            for (Vm vm : host.getVmList()) {
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                        cloudletFinished++;
                        if (cloudletFinished % 10000 == 0) {
                            System.out.println("Cloudlet " + cloudletFinished + " finished at " + CloudSim.clock());
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds a file into the resource's storage before the experiment starts. If
     * the file is a master file, then it will be registered to the RC when the
     * experiment begins.
     *
     * @param file a DataCloud file
     * @return a tag number denoting whether this operation is a success or not
     */
    public int addFile(File file) {
        if (file == null) {
            return DataCloudTags.FILE_ADD_ERROR_EMPTY;
        }

        if (contains(file.getName())) {
            return DataCloudTags.FILE_ADD_ERROR_EXIST_READ_ONLY;
        }

        // check storage space first
        if (getStorageList().size() <= 0) {
            return DataCloudTags.FILE_ADD_ERROR_STORAGE_FULL;
        }

        Storage tempStorage = null;
        int msg = DataCloudTags.FILE_ADD_ERROR_STORAGE_FULL;

        for (int i = 0; i < getStorageList().size(); i++) {
            tempStorage = getStorageList().get(i);
            if (tempStorage.getAvailableSpace() >= file.getSize()) {
                tempStorage.addFile(file);
                msg = DataCloudTags.FILE_ADD_SUCCESSFUL;
                break;
            }
        }

        return msg;
    }

    /**
     * Checks whether the resource has the given file.
     *
     * @param file a file to be searched
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean contains(File file) {
        if (file == null) {
            return false;
        }
        return contains(file.getName());
    }

    /**
     * Checks whether the resource has the given file.
     *
     * @param fileName a file name to be searched
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean contains(String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return false;
        }

        Iterator<Storage> it = getStorageList().iterator();
        Storage storage = null;
        boolean result = false;

        while (it.hasNext()) {
            storage = it.next();
            if (storage.contains(fileName)) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Deletes the file from the storage. Also, check whether it is possible to
     * delete the file from the storage.
     *
     * @param fileName the name of the file to be deleted
     * @return the error message
     */
    private int deleteFileFromStorage(String fileName) {
        Storage tempStorage = null;
        File tempFile = null;
        int msg = DataCloudTags.FILE_DELETE_ERROR;

        for (int i = 0; i < getStorageList().size(); i++) {
            tempStorage = getStorageList().get(i);
            tempFile = tempStorage.getFile(fileName);
            tempStorage.deleteFile(fileName, tempFile);
            msg = DataCloudTags.FILE_DELETE_SUCCESSFUL;
        } // end for

        return msg;
    }

    /*
	 * (non-Javadoc)
	 * @see cloudsim.core.SimEntity#shutdownEntity()
     */
    @Override
    public void shutdownEntity() {
        if (mainStorage) {
            return;
        }
        for (Cache c : caches) {
            Log.cacheEnd(getId(), c.dataObjectID, c.length);
        }
        //Log.printLine(getName() + " is shutting down...");
    }

    /*
	 * (non-Javadoc)
	 * @see cloudsim.core.SimEntity#startEntity()
     */
    @Override
    public void startEntity() {
        //Log.printLine(getName() + " is starting...");
        // this resource should register to regional GIS.
        // However, if not specified, then register to system GIS (the
        // default CloudInformationService) entity.
        int gisID = CloudSim.getEntityId(regionalCisName);
        if (gisID == -1) {
            gisID = CloudSim.getCloudInfoServiceEntityId();
        }

        // send the registration to GIS
        sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());
        // Below method is for a child class to override
        registerOtherEntity();
    }

    /**
     * Gets the host list.
     *
     * @return the host list
     */
    @SuppressWarnings("unchecked")
    public <T extends Host> List<T> getHostList() {
        return (List<T>) getCharacteristics().getHostList();
    }

    /**
     * Gets the characteristics.
     *
     * @return the characteristics
     */
    protected DatacenterCharacteristics getCharacteristics() {
        return characteristics;
    }

    /**
     * Sets the characteristics.
     *
     * @param characteristics the new characteristics
     */
    protected void setCharacteristics(DatacenterCharacteristics characteristics) {
        this.characteristics = characteristics;
    }

    /**
     * Gets the regional cis name.
     *
     * @return the regional cis name
     */
    protected String getRegionalCisName() {
        return regionalCisName;
    }

    /**
     * Sets the regional cis name.
     *
     * @param regionalCisName the new regional cis name
     */
    protected void setRegionalCisName(String regionalCisName) {
        this.regionalCisName = regionalCisName;
    }

    /**
     * Gets the vm allocation policy.
     *
     * @return the vm allocation policy
     */
    public VmAllocationPolicy getVmAllocationPolicy() {
        return vmAllocationPolicy;
    }

    /**
     * Sets the vm allocation policy.
     *
     * @param vmAllocationPolicy the new vm allocation policy
     */
    protected void setVmAllocationPolicy(VmAllocationPolicy vmAllocationPolicy) {
        this.vmAllocationPolicy = vmAllocationPolicy;
    }

    /**
     * Gets the last process time.
     *
     * @return the last process time
     */
    protected double getLastProcessTime() {
        return lastProcessTime;
    }

    /**
     * Sets the last process time.
     *
     * @param lastProcessTime the new last process time
     */
    protected void setLastProcessTime(double lastProcessTime) {
        this.lastProcessTime = lastProcessTime;
    }

    /**
     * Gets the storage list.
     *
     * @return the storage list
     */
    protected List<Storage> getStorageList() {
        return storageList;
    }

    /**
     * Sets the storage list.
     *
     * @param storageList the new storage list
     */
    protected void setStorageList(List<Storage> storageList) {
        this.storageList = storageList;
    }

    /**
     * Gets the vm list.
     *
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends Vm> List<T> getVmList() {
        return (List<T>) vmList;
    }

    /**
     * Sets the vm list.
     *
     * @param vmList the new vm list
     */
    protected <T extends Vm> void setVmList(List<T> vmList) {
        this.vmList = vmList;
    }

    /**
     * Gets the scheduling interval.
     *
     * @return the scheduling interval
     */
    protected double getSchedulingInterval() {
        return schedulingInterval;
    }

    /**
     * Sets the scheduling interval.
     *
     * @param schedulingInterval the new scheduling interval
     */
    protected void setSchedulingInterval(double schedulingInterval) {
        this.schedulingInterval = schedulingInterval;
    }

    public void setBindedBR(int bindedBR) {
        this.bindedBR = bindedBR;
    }

    public int getBindedBR() {
        return bindedBR;
    }

    protected static class Cache {

        private final int dataObjectID;
        private final int length;
        private Cloudlet c;

        private HashSet<Integer> recentRequestors;

        public Cache(int dataObjectID, int length) {
            this.dataObjectID = dataObjectID;
            this.length = length;
            c = null;
            recentRequestors = new HashSet<>();
        }

        public Cache(int dataObjectID, int length, Cloudlet c) {
            this(dataObjectID, length);
            this.c = c;
        }

        public HashSet<Integer> getRecentRequestors() {
            return recentRequestors;
        }

        public void addRequestor(int r) {
            recentRequestors.add(r);
        }

        public boolean isLocalCache() {
            return CloudSim.isCacheEnabled() && null != c;
        }

        public Cloudlet getCloudlet() {
            return c;
        }

        public int getDataObjectID() {
            return dataObjectID;
        }

        public int getLength() {
            return length;
        }

        @Override
        public int hashCode() {
            return dataObjectID;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Cache other = (Cache) obj;
            if (this.dataObjectID != other.dataObjectID) {
                return false;
            }
            return true;
        }

    }

    private static class Request {

        private final double time;
        private final double latency;
        private final int originalSource;
        private final int neighbourSource;
        private final int destination;
        private final int dataObjectID;
        private final int length;

        public Request(double creationTime, int source, int destination, int dataObjectID, int length) {
            time = CloudSim.clock();
            this.latency = time - creationTime;
            this.originalSource = source;
            neighbourSource = NetworkTopology.getSourceNeighbour(source, destination);
            this.destination = destination;
            this.dataObjectID = dataObjectID;
            this.length = length;
        }
    }

}
