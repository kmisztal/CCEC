package cec.input;

import cec.cluster.ClusterLike;
import cec.cluster.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author Krzysztof
 */
public class Data {

    private List<ClusterLike> data;

    public void read(String filename, String type) throws IOException {
        String separator = "";
        switch (type) {
            case "text/tab-separated-values":
                separator = "\t";
                break;
            case "text/space-separated-values":
                separator = " ";
                break;
            default:
                throw new RuntimeException("File type error");
        }

        final double weight = 1. / countLines(filename);
        
        FileReader fileReader = new FileReader(filename);

        final int dim;
        try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            data = new CopyOnWriteArrayList<>();
            String line = null;
            if ((line = bufferedReader.readLine()) != null) {
                dim = getDimension(line, separator);
            } else {
                throw new RuntimeException("Empty file " + filename);
            }

            do {
                final String[] ls = line.split(separator);
                if (dim != ls.length) {
                    throw new RuntimeException("Dimension error in data file");
                } else {
                    data.add(new Point(weight, ls));
                }
            }while ((line = bufferedReader.readLine()) != null);
        }
    }

    private int countLines(String filename) throws IOException {
        LineNumberReader reader = new LineNumberReader(new FileReader(filename));
        int cnt = 0;
        String lineRead = "";
        while ((lineRead = reader.readLine()) != null) {
        }

        cnt = reader.getLineNumber();
        reader.close();
        return cnt;
    }

    private static int getDimension(String line, String separator) {
        return line.split(separator).length;
    }

    /**
     * Returns an iterator over a set of elements of type T.
     *
     * @return an Iterator.
     */
    public Iterator<ClusterLike> iterator() {
        return data.iterator();
    }

    public int getSize() {
        return data.size();
    }

    public int getDimension() {
        return data.get(0).getDimension();
    }

    public List<ClusterLike> getData() {
        return data;
    }

    public ClusterLike get(int i) {
        return data.get(i);
    }

    @Override
    public String toString() {
        if(data.isEmpty())
            return "Empty dataset";
        return "Data: size => " + getSize() + "; dimension => " + getDimension();
    }

    public Data(List<ClusterLike> data) {
        this.data = data;
    }

    public Data() {
    }
    
    
    
    
}
