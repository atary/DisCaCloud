/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parsers;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 *
 * @author ataka
 */
public class GeographicalLocality {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        int numRecords = 100;
        //double distanceThreshold = 100;

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

                InetAddress clientIP = InetAddress.getByName(w.getClientID());

                CityResponse clientCity;
                try {
                    clientCity = reader.city(clientIP);
                    locations.add(clientCity.getLocation());
                } catch (Exception ex) {
                }
            }

            for (int i = 0; i < locations.size(); i++) {
                Location l1 = locations.get(i);
                double lat1 = l1.getLatitude();
                double lon1 = l1.getLongitude();
                for (int j = i + 1; j < locations.size(); j++) {
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
                    System.out.println(distance);
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
