package parsers;

public abstract class RequestDatum {

    protected long reqTime;
    protected String clientID;
    protected String serverID;
    protected int length;

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
}
