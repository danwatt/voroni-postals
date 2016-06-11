package org.danwatt.voronipostals;

import java.util.Objects;

public class PostalCode implements Cloneable {
    public String country;
    public String postal;
    public String city;
    public String state;
    public String county;
    public double latitude;
    public double longitude;
    public String wkt;
    public String color;

    public static PostalCode loadLine(String line) {
        String[] parts = line.split("\t");
        PostalCode pc = new PostalCode();
        pc.country = parts[0];
        pc.postal = parts[1];
        pc.city = parts[2];
        pc.state = parts[3];
        pc.county = parts[5];
        pc.latitude = Double.parseDouble(parts[9]);
        pc.longitude = Double.parseDouble(parts[10]);
        return pc;
    }

    @Override
    public String toString() {
        return postal;
    }

    @Override
    public boolean equals(Object o) {
        return postal.equals(((PostalCode)o).postal);
    }

    @Override
    public int hashCode() {
        return postal.hashCode();
    }

    @Override
    public PostalCode clone() {
        try {
            return (PostalCode) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
