package org.danwatt.voronipostals;

import org.rapidoid.annotation.Controller;
import org.rapidoid.annotation.GET;
import org.rapidoid.annotation.Page;

import java.util.List;

@Controller
public class VoroniController {

    @Page("/map")
    public Object map() {
        return "";
    }

    @GET("/nearby/counties/{point}")
    public List<PostalCode> nearby(String point) {
        String[] parts = point.split(",");
        double lat = Double.parseDouble(parts[0]);
        double lon = Double.parseDouble(parts[1]);
        return PostalSource.getInstance().getNearbyCounties(lat,lon);
    }

    @GET("/nearby/postals/{point}")
    public List<PostalCode> nearbyPostals(String point) {
        String[] parts = point.split(",");
        double lat = Double.parseDouble(parts[0]);
        double lon = Double.parseDouble(parts[1]);
        return PostalSource.getInstance().getPostalsAroundPoint(lat,lon);
    }
}
