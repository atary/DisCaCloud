package parsers;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author ataka
 */
public class GeographicalLocality {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        int numRecords = 1000000;
        //double distanceThreshold = 100;
        HashSet<String> uids = new HashSet<>();
        ArrayList<String> ids = new ArrayList<>();
        for (double distanceThreshold = 100; distanceThreshold <= 100; distanceThreshold += 100) {

            int closeCount = 0;
            int sameCount = 0;
            int totalCount = 0;
            String requestFile = "wSharkLogs/juice1M.txt";
            ArrayList<Location> locations = new ArrayList<>();
            RequestTextReaderInterface wsReader = new WSharkTextReader();
            File GeoDatabase = null;
            DatabaseReader reader = null;
            GeoDatabase = new File("C:\\GeoLite2-City.mmdb");
            reader = new DatabaseReader.Builder(GeoDatabase).build();
            wsReader.open(requestFile);

            for (RequestDatum w : wsReader.readNRecords(numRecords)) {
                
                ids.add(w.getClientID());
                uids.add(w.getClientID());

                /*InetAddress clientIP = InetAddress.getByName(w.getClientID());

                CityResponse clientCity;
                try {
                    clientCity = reader.city(clientIP);
                    locations.add(clientCity.getLocation());
                    //System.out.println(clientCity.getLocation().getLatitude() + "\t" + clientCity.getLocation().getLongitude());
                } catch (Exception ex) {
                }*/
            }
            if(true){
                System.out.println(uids.size());
                System.out.println(ids.size());
                return;
            }
            Path p = Paths.get("C:\\distances.txt");
            int count = (locations.size() * (locations.size() - 1)) / 2;
            int k = 0;
            System.out.println(count);
            int prev = 0;
            for (int i = 0; i < locations.size(); i++) {
                Location l1 = locations.get(i);
                double lat1 = l1.getLatitude();
                double lon1 = l1.getLongitude();
                for (int j = i + 1; j < locations.size(); j++) {
                    k++;
                    if (k * 100 / count > prev) {
                        System.out.println(++prev + "%");
                    }
                    totalCount++;
                    Location l2 = locations.get(j);
                    double lat2 = l2.getLatitude();
                    double lon2 = l2.getLongitude();
                    double distance = HaversineDistance(lat1, lat2, lon1, lon2);
                    if (lat1 == lat2 && lon1 == lon2) {
                        sameCount++;
                    } else if (distance < distanceThreshold) {
                        closeCount++;
                    }
                    //System.out.println(distance);
                    String text = distance + "\n";
                    Files.write(p, text.getBytes(), StandardOpenOption.APPEND);
                }
            }
            //System.out.println(distanceThreshold + ": " + closeCount + "/" + totalCount + " (" + (closeCount * 100 / totalCount) + ")");
            //System.out.println(distanceThreshold + ": " + sameCount + "/" + totalCount + " (" + (sameCount * 100 / totalCount) + ")");
        }
    }

    private static double HaversineDistance(double lat1, double lat2, double lon1, double lon2) {
        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; //KM
    }
}
