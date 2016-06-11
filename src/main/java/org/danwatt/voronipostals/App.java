package org.danwatt.voronipostals;


import org.rapidoid.setup.On;
import org.rapidoid.web.Rapidoid;

public class App {
    public static void main(String[] args) {
        PostalSource.getInstance();
        On.port(Integer.parseInt(args[0]));
        Rapidoid.run(args);
    }
}
