package builder

/*
    DSL для описания команд Maven.
    Общая структура:
    mvn[<operation_prefix><operation_name>[<option_prefix><option_key>[<option_separator><option_value>](0+)](0+)]
    На уровне операций задаются команды (этапы) сборки, а также глобальные определения -D.
    На уровне опций задаются аргументы в виде ключей с возможными значениями.
    Команд и операций может быть несколько.
    Можно создать собственные операции через custom и опции через option.
    По умолчанию префикс операции " ", префикс опции " -", разделитель опции " ", опция задана только ключом.
    mvn{
        define(<глобальный аргумент, задаваемый -D>)
        define(...)
        package{
            define(<локальный аргумент -D только для данной команды>)
            path(<путь сборки>)
            option(<ключ>)
            option(<ключ>, <значение>)
            ...
        }
        custom(<этап сборки / команда>){
            option(...)
            ...
        }
    }
    Объект преобразовывается в команду через toString().
 */

interface Renderable {
    fun render(builder: StringBuilder)
}

abstract class MavenOperation(val name: String, val prefix: String = " "): Renderable{
    private val options = arrayListOf<OperationOption>()

    protected fun <T: OperationOption> initOption(option: T, init: T.() -> Unit): T{
        option.init()
        options.add(option)
        return option
    }

    override fun render(builder: StringBuilder) {
        builder.append("${this.prefix}${this.name}")
        for (option in options) {
            option.render(builder)
        }
    }

    fun option(key: String, value: String?=null,
               prefix: String = " -", separator: String = " ") = initOption(Option(key, value, prefix, separator)) {}
    fun path(path: String) = initOption(Path(path = path)) {}
    fun define(property: String) = initOption(Define(property = property)) {}
}

class MavenCustomOperation(name: String, prefix: String = " "): MavenOperation(name, prefix)
class MavenPackage: MavenOperation("package")
class MavenTest: MavenOperation("test")
class MavenClean: MavenOperation("clean")
class MavenGlobalDefine: MavenOperation("-D")

abstract class OperationOption(val key: String,
                               val value: String?=null,
                               val prefix: String = " -",
                               val separator: String = " "): Renderable{
    override fun render(builder: StringBuilder){
        if (value != null)
            builder.append("${this.prefix}${this.key}${this.separator}${this.value}")
        else
        builder.append("${this.prefix}${this.key}")
    }
}

class Option(key: String, value: String?, prefix: String = " -", separator: String = " "):
        OperationOption(key, value, prefix, separator)
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
    fun define(argument: String) = initOperation(MavenGlobalDefine(), {
        option(argument, prefix="")
    })
    fun custom(
            operation: String,
            prefix: String = " ",
            init: MavenCustomOperation.() -> Unit = {}
    ) = initOperation(MavenCustomOperation(operation, prefix), init)
}

fun mvn(init: Mvn.() -> Unit): Mvn {
    val mvn = Mvn()
    mvn.init()
    return mvn
}
