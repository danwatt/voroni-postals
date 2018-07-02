package org.danwatt.voronipostals


import org.rapidoid.setup.On
import org.rapidoid.web.Rapidoid
import java.util.*

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        Objects.requireNonNull(args[0])

        PostalSource.instance
        On.port(Integer.parseInt(args[0]))
        Rapidoid.run(*args)
    }
}
