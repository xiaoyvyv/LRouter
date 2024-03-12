package com.aleyn.router.inject

import androidx.annotation.Keep

@Keep
interface IRouterGenerate {
    @Keep
    fun injectAutowired(target: Any?)

    @Keep
    fun initModuleRouter()

    @Keep
    fun registerIntercept()

    @Keep
    fun registerAllInitializer()
}