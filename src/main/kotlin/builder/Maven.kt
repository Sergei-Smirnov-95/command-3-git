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
    fun path(path: String) = initOption(builder.Path(path = path)) {}
}

class MavenPackage: MavenOperation("package")
class MavenTest: MavenOperation("test")
class MavenClean: MavenOperation("clean")

abstract class OperationOption(val key: String, val value: String?=null): Renderable{
    override fun render(builder: StringBuilder){
        if (value != null)
            builder.append(" -${this.key} ${this.value}")
        else
            builder.append(" -${this.key}")
    }
}

class Path(path: String): OperationOption(key = "f", value = path)
class Option(key: String, value: String?): OperationOption(key, value)

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

    fun test(init: MavenTest.() -> Unit) = initOperation(MavenTest(), init)
    fun clean(init: MavenClean.() -> Unit) = initOperation(MavenClean(), init)
    fun pckg(init: MavenPackage.() -> Unit) = initOperation(MavenPackage(), init)
}

fun mvn(init: Mvn.() -> Unit): Mvn {
    val mvn = Mvn()
    mvn.init()
    return mvn
}
