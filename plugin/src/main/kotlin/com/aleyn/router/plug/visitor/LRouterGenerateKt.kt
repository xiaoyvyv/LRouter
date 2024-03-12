package com.aleyn.router.plug.visitor

object LRouterGenerateKt {
    fun injectAutowired(obj: Any?) {
        try {
            VoiceConversationActivity.autowiredInject(obj)
        } catch (unused: Exception) {
        }
    }
}

object VoiceConversationActivity {
    @JvmStatic
    fun autowiredInject(obj: Any?) {
    }
}