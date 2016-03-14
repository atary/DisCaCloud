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

    public RequestDatum(String str) {
        String[] s = str.split("\t");
        reqTime = Long.parseLong(s[0]);
        clientID = s[1];
        serverID = s[2];
        length = Integer.parseInt(s[3]);
        //length = length == 0 ? 1 : (int) Math.log(length);
    }

    public RequestDatum() {
    }

}
