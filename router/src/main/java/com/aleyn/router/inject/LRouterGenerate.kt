package com.aleyn.router.inject

import androidx.annotation.Keep

private val instance: IRouterGenerate by lazy {
    Class.forName("com.router.RouterGenerateHolder")
        .getDeclaredConstructor()
        .newInstance() as IRouterGenerate
}

@Keep
internal fun injectAutowired(target: Any?) {
    instance.injectAutowired(target)
}

@Keep
internal fun initModuleRouter() {
    instance.initModuleRouter()
}

@Keep
internal fun registerIntercept() {
    instance.registerIntercept()
}

@Keep
internal fun registerAllInitializer() {
    instance.registerAllInitializer()
}