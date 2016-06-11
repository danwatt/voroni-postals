package org.danwatt.voronipostals;


import org.rapidoid.web.Rapidoid;

public class App {
    public static void main(String[] args) {
        PostalSource.getInstance();
        Rapidoid.run(args);
    }
}
