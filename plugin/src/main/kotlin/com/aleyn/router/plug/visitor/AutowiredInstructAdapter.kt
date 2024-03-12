package com.aleyn.router.plug.visitor

import com.aleyn.router.plug.data.HandleModel
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.InstructionAdapter


/**
 * @author: Aleyn
 * @date: 2023/7/10 9:50
 */
class AutowiredInstructAdapter(
    api: Int,
    methodVisitor: MethodVisitor,
    private val autowiredClass: List<HandleModel.Autowired>?
) : InstructionAdapter(api, methodVisitor) {

    override fun visitCode() {
        autowiredClass?.forEach {
            val start = Label()
            val end = Label()
            val handler = Label()

            visitTryCatchBlock(start, end, handler, "java/lang/Exception")
            mark(start)
            nop()
            load(1, OBJECT_TYPE)
            invokestatic(
                it.className,
                "autowiredInject",
                "(Ljava/lang/Object;)V",
                false
            )
            mark(end)

            val label = Label()
            goTo(label)
            mark(handler)
            store(2, OBJECT_TYPE)
            mark(label)
        }
        super.visitCode()
    }

}