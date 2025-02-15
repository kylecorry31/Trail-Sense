package com.kylecorry.trail_sense.shared.extensions

// TODO: ANDROMEDA
object AppServiceRegistry {

    val services = mutableMapOf<String, Any>()

    inline fun <reified T : Any> register(service: T) {
        services[T::class.java.name] = service
    }

    inline fun <reified T : Any> get(): T {
        return services[T::class.java.name] as? T
            ?: throw Exception("Service is not of the correct type")
    }

}