package util

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
//import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.CoroutineName
import java.lang.Error
import java.lang.reflect.Method
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KFunction

/******************************************************************************/

//suspend fun currentCoroutineName() =
//        suspendCoroutine<CoroutineContext> { c -> c.resume(c.context) }[CoroutineName.Key]
//                ?: CoroutineName(newRequestUUID())

/******************************************************************************/

/*
 О том, как использовать. В vx-ах в качестве параметра задается тип обработчика. Оборачиваемая функция vertx должна
 принимать обработчик указанного типа. Например, Vertx.DeployVerticle принимает Handler<AsyncResult<String>>, т.е.
 после развертывания вертикали вернется AsyncResult, у которого в результате String - идентификатор включенной вертикали.
 Результат из AsyncResult'а потом достается через метод result(). В vxt и vxu результат уже вынут.
 Т.е. если обернуть вызов в vx, то результат выполнения можно сразу присвоить, несмотря на асинхронность.
 В последующих строчках результат точно будет, т.к. все ожидания съедены корутинной магией - нужное место проснулось
 тогда, когда результат появился.
*/
/*
 О том, как это работает.
 Связыввается появление результата в Vertx и пробуждение корутины.
 Handler внутри определяет метод handle (один в интерфейсе - задан лямбдой).
 Реализация handle говорит, что при возникновении события (vertx), нужно вызвать продолжение корутины с этим событием.
 Если внутри callback функции cb нет вызова Vertx, гарантирующего, что cb будет вызван 1 раз, то все зависнет,
 т.к. cb не будет выполнен, из-за этого привязки к возникновению результата не будет. Если
 vertx дернет cb больше одного раза, то все упадет, когда будет двойной resume из одной точки продолжения.
 Вроде бы, последнего случаться не должно, Vertx использует обработчики один раз.
*/

val vertx = Vertx.vertx()
val eb = vertx.eventBus()

inline suspend fun vxu(crossinline cb: (Handler<AsyncResult<Void?>>) -> Unit): Void? =
        suspendCoroutine { cont ->
            cb(Handler { res ->
                if (res.succeeded()) cont.resume(res.result())
                else cont.resumeWithException(res.cause())
            })
        }

inline suspend fun <T> vxt(crossinline cb: (Handler<T>) -> Unit): T =
        suspendCoroutine { cont ->
            cb(Handler { res -> cont.resume(res) })
        }

inline suspend fun <T> vxa(crossinline cb: (Handler<AsyncResult<T>>) -> Unit): T =
        suspendCoroutine { cont ->
            cb(Handler { res ->
                if (res.succeeded()) cont.resume(res.result())
                else cont.resumeWithException(res.cause())
            })
        }

/******************************************************************************/

class WithExceptionsContext(val handler: (Throwable) -> Unit) :
        AbstractCoroutineContextElement(CoroutineExceptionHandler.Key),
        CoroutineExceptionHandler
//        Loggable
{
    override fun handleException(context: CoroutineContext, exception: Throwable) =
            handler(exception)
}


suspend fun <ReturnType> EventBus.sendAsync(address: String, message: Any, deliveryOptions: DeliveryOptions = DeliveryOptions()): Message<ReturnType> {
    return vxa { send(address, message, deliveryOptions, it) }
}





/*
fun Loggable.LogExceptions() = WithExceptionsContext(
        { log.error("Oops!", it) }
)

fun Loggable.WithExceptions(handler: (Throwable) -> Unit) = WithExceptionsContext(
        { handler(it) }
)

fun <U> Loggable.WithExceptions(handler: Handler<AsyncResult<U>>) = WithExceptionsContext(
        { handleException(handler, it) }
)

fun <U> Loggable.WithExceptions(msg: Message<U>) = WithExceptionsContext(
        { handleException(msg, it) }
)

fun Loggable.WithExceptions(ctx: RoutingContext) = WithExceptionsContext(
        { handleException(ctx, it) }
)
*/

// NOTE: suspendCoroutineOrReturn<> is not recommended by kotlin devs, BUT,
// however, suspendCoroutine<>, the only alternative, does *not* work correctly if suspend fun has no
// suspension points.

/*
inline suspend fun <R> KFunction<R>.callAsync(vararg args: Any?) =
        when {
            isSuspend -> suspendCoroutineOrReturn<R> { call(*args, it) }
            else -> throw Error("$this cannot be called as async")
        }

val Method.isKotlinSuspend
    get() = parameters.lastOrNull()?.type == kotlin.coroutines.experimental.Continuation::class.java

inline suspend fun Method.invokeAsync(receiver: Any?, vararg args: Any?) =
        when {
            isKotlinSuspend -> suspendCoroutineOrReturn<Any?> { invoke(receiver, *args, it) }
            else -> throw Error("$this cannot be invoked as async")
        }
*/