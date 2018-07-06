package org.danwatt.voronipostals

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.ParseException
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier
import org.apache.commons.io.FileUtils

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

import java.nio.file.Files.readAllLines
import java.util.Collections.emptyMap

class UsBorderBuilder {

    @Throws(ParseException::class, IOException::class, URISyntaxException::class)
    fun boundaryOnly() {
        val less = buildSimplifiedUsBoundary()
        FileUtils.writeStringToFile(File("/tmp/out.wk"), less.toText(), Charset.forName("UTF-8"))
    }

    companion object {

        val US_BORDER_EXPANSION = 0.06
        val US_BORDER_SIMPLIFICATION = 0.025
        val HOLE_TOLERANCE = 0.05

        @Throws(IOException::class, URISyntaxException::class, ParseException::class)
        internal fun buildSimplifiedUsBoundary(): Geometry {
            val canada = loadFile("can.wkt")
            val mexico = loadFile("mex.wkt")
            var america = loadFile("us.wkt")

            america = DouglasPeuckerSimplifier.simplify(america.buffer(US_BORDER_EXPANSION), US_BORDER_SIMPLIFICATION)
                    .difference(canada)
                    .difference(mexico)

            var usGeom: Geometry = GeoUtils.lowPrecisionFactory.createGeometry(america)!!
            usGeom = HoleRemover.cleanSmallHoles(usGeom, HOLE_TOLERANCE)
            return usGeom.buffer(0.0).union()
        }

        @Throws(IOException::class, URISyntaxException::class, ParseException::class)
        private fun loadFile(file: String): Geometry {
            val path = GeoUtils.getPath(file)
            val usWkt = readAllLines(path)[0]
            return GeoUtils.reader.read(usWkt)
        }
    }
}
