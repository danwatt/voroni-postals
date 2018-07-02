package org.danwatt.voronipostals

sealed class GeoContainer {
    var wkt: String? = null
    var color: String? = null
}
data class County(
        val state: String,
        val county: String
) : GeoContainer()

data class PostalCode(val country: String = "",
                      val postal: String = "",
                      val city: String = "",
                      val state: String = "",
                      val county: String = "",
                      val latitude: Double = 0.toDouble(),
                      val longitude: Double = 0.toDouble()) : Cloneable, GeoContainer() {

    override fun toString(): String = postal
    override fun equals(other: Any?): Boolean = postal == (other as PostalCode).postal
    override fun hashCode(): Int = postal.hashCode()

    companion object {
        fun loadLine(line: String): PostalCode {
            val parts = line.split("\t".toRegex()).dropLastWhile(String::isEmpty)
            return PostalCode(country = parts[0],
                    postal = parts[1],
                    city = parts[2],
                    state = parts[3],
                    county = parts[5],
                    latitude = java.lang.Double.parseDouble(parts[9]),
                    longitude = java.lang.Double.parseDouble(parts[10]))
        }
    }
}

