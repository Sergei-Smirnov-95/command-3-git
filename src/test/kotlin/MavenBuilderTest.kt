package test

import builder.mvn
import org.junit.Test
import kotlin.test.assertEquals

class MavenBuilderTest {

    @Test
    fun testEmpty() {
        val res = mvn { }
        assertEquals("mvn", res.toString())
    }

    @Test
    fun testSingle() {
        val res = mvn { clean {} }
        assertEquals("mvn clean", res.toString())
    }

    @Test
    fun testMultipleNoOptions() {
        val res = mvn {
            clean {}
            pckg {}
            test {}
        }
        assertEquals("mvn clean package test", res.toString())
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
        assertEquals("mvn clean -e -ff package -f /some/path -o test -key value", res.toString())
    }

    @Test
    fun testDefine() {
        val res = mvn {
            custom("help:describe") {
                define("cmd=compiler:compile")
            }
            custom("install") {
                define("maven.test.skip=true")
            }
        }
        assertEquals("mvn help:describe -Dcmd=compiler:compile install -Dmaven.test.skip=true", res.toString())
    }

}

