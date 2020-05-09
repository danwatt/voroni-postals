package org.danwatt.voronipostals


import org.rapidoid.setup.App
import org.rapidoid.setup.On
import org.rapidoid.web.Rapidoid
import java.util.*
import kotlin.system.exitProcess

object App {
    @JvmStatic
    fun main(args: Array<String?>) {
        PostalSource.instance
        App.bootstrap(args)
    }
}
