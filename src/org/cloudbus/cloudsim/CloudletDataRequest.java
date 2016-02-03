/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cloudbus.cloudsim;

/**
 *
 * @author Atakan
 * Which data will be required in which position of execution.
 */
class CloudletDataRequest {
    private int dataObjectID;
    private long cloudletLenghtPos;
    private boolean requested;

    public CloudletDataRequest(int dataObjectID, long cloudletLenghtPos) {
        this.dataObjectID = dataObjectID;
        this.cloudletLenghtPos = cloudletLenghtPos;
        requested = false;
    }

    public int getDataObjectID() {
        return dataObjectID;
    }

    public long getCloudletLenghtPos() {
        return cloudletLenghtPos;
    }

    public boolean isRequested() {
        return requested;
    }

    public void setRequested() {
        requested = true;
    }
   
}
