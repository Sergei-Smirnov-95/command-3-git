package builder

interface Renderable {
    fun render(builder: StringBuilder)
}

abstract class MavenOperation(val name: String): Renderable{
    private val options = arrayListOf<OperationOption>()

    protected fun <T: OperationOption> initOption(option: T, init: T.() -> Unit): T{
        option.init()
        options.add(option)
        return option
    }

    override fun render(builder: StringBuilder) {
        builder.append(" ${this.name}")
        for (option in options) {
            option.render(builder)
        }
    }

    fun option(key: String, value: String?=null) = initOption(Option(key, value)) {}
    fun path(path: String) = initOption(Path(path = path)) {}
    fun define(property: String) = initOption(Define(property = property)) {}
}

class MavenCustomOperation(name: String): MavenOperation(name)
class MavenPackage: MavenOperation("package")
class MavenTest: MavenOperation("test")
class MavenClean: MavenOperation("clean")

abstract class OperationOption(val key: String, val value: String?=null, val separator: String = " "): Renderable{
    override fun render(builder: StringBuilder){
        if (value != null)
            builder.append(" -${this.key}${this.separator}${this.value}")
        else
            builder.append(" -${this.key}")
    }
}

class Option(key: String, value: String?, separator: String = " "): OperationOption(key, value, separator)
class Path(path: String): OperationOption(key = "f", value = path)
class Define(property: String): OperationOption(key = "D", value = property, separator = "")

class Mvn: Renderable {
    val opertaions = arrayListOf<MavenOperation>()

    override fun render(builder: StringBuilder){
        builder.append("mvn")
        for (operation in opertaions){
            operation.render(builder)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder("")
        render(builder)
        return builder.toString()
    }

    protected fun <T: MavenOperation> initOperation(operation: T, init: T.() -> Unit): T{
        operation.init()
        opertaions.add(operation)
        return operation
    }

    fun test(init: MavenTest.() -> Unit = {}) = initOperation(MavenTest(), init)
    fun clean(init: MavenClean.() -> Unit = {}) = initOperation(MavenClean(), init)
    fun pckg(init: MavenPackage.() -> Unit = {}) = initOperation(MavenPackage(), init)
    fun custom(
            operation: String,
            init: MavenCustomOperation.() -> Unit = {}
    ) = initOperation(MavenCustomOperation(operation), init)
}

fun mvn(init: Mvn.() -> Unit): Mvn {
    val mvn = Mvn()
    mvn.init()
    return mvn
}
