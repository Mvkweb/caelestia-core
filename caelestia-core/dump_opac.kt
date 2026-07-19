import java.net.URLClassLoader
import java.io.File
fun main() {
    val cl = URLClassLoader(arrayOf(File("libs/opac-api.jar").toURI().toURL()))
    val clazz = cl.loadClass("xaero.pac.common.server.api.OpenPACServerAPI")
    for (m in clazz.declaredMethods) {
        println(m)
    }
    for (f in clazz.declaredFields) {
        println(f)
    }
}
