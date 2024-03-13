package com.aleyn.router.plug.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RouterStubClassTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputFolder: DirectoryProperty

    @TaskAction
    fun taskAction() {
        val outputFile = File(outputFolder.asFile.get(), "com/router/RouterGenerateHolder.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            "package com.router\n" +
                    "\n" +
                    "import androidx.annotation.Keep\n" +
                    "import com.aleyn.router.inject.IRouterGenerate\n" +
                    "\n" +
                    "/**\n" +
                    " * 插桩类，自动生成\n" +
                    " */\n" +
                    "@Keep\n" +
                    "class RouterGenerateHolder : IRouterGenerate {\n" +
                    "\n" +
                    "    @Keep\n" +
                    "    override fun injectAutowired(target: Any?) {\n" +
                    "        // 插桩占位\n" +
                    "    }\n" +
                    "\n" +
                    "    @Keep\n" +
                    "    override fun initModuleRouter() {\n" +
                    "        // 插桩占位\n" +
                    "    }\n" +
                    "\n" +
                    "    @Keep\n" +
                    "    override fun registerIntercept() {\n" +
                    "        // 插桩占位\n" +
                    "    }\n" +
                    "\n" +
                    "    @Keep\n" +
                    "    override fun registerAllInitializer() {\n" +
                    "        // 插桩占位\n" +
                    "    }\n" +
                    "}"
        )
    }
}