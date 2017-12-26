package test

import builder.mvn
import org.junit.Test
import kotlin.test.assertEquals

class MavenBuilderTest {

    @Test
    fun testEmpty() {
        val res = mvn { }
        assertEquals("mvn", res.getString("command"))
    }

    @Test
    fun testSingle() {
        val res = mvn { clean {} }
        assertEquals("mvn clean", res.getString("command"))
    }

    @Test
    fun testMultipleNoOptions() {
        val res = mvn {
            clean {}
            pckg {}
            test {}
        }
        assertEquals("mvn clean package test", res.getString("command"))
    }

    @Test
    fun testMultipleOperationsWithOptions() {
        val res = mvn {
            clean {
                option("e")
                option("ff")
            }
            pckg {
                path("/some/path")
                option("o")
            }
            test {
                option("key", "value")
            }
        }
        assertEquals("mvn clean -e -ff package -f /some/path -o test -key value", res.getString("command"))
    }
}

