/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parsers;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ataka
 */
public interface RequestTextReaderInterface {

    public ArrayList<RequestDatum> read(String aFileName) throws FileNotFoundException, ParseException;

    public void open(String fName) throws FileNotFoundException;

    public boolean hasNext();

    public List<RequestDatum> readNRecords(int limit);
}
