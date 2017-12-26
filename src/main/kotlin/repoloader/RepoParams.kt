package repoloader

import io.vertx.core.json.JsonObject

class RepoParams {
    open val paramsObj = JsonObject()

    protected fun addParam(key: String, value: String): Unit{
        paramsObj.put(key, value)
    }

    fun url(value: String) = addParam("url", value)
    fun branch(value: String) = addParam("branch", value)
    fun type(value: String) = addParam("type", value)
}

fun repo(addParam: RepoParams.() -> Unit): JsonObject {
    val repo = RepoParams()
    repo.addParam()
    return repo.paramsObj
}