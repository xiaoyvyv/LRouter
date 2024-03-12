package com.aleyn.router.plug.task

import com.aleyn.router.plug.data.HandleModel
import com.aleyn.router.plug.visitor.FindHandleClass
import com.aleyn.router.plug.visitor.InsertCodeVisitor
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * @author: Aleyn
 * @date: 2023/7/14 11:03
 */

/**
 * 待插桩类
 */
internal const val ROUTER_INJECT = "com/router/RouterGenerateHolder.class"

val handleModels = arrayListOf<HandleModel>()

abstract class LRouterClassTask : DefaultTask() {

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @Internal
    val jarPaths = mutableSetOf<String>()

    @TaskAction
    fun taskAction() {
        val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile)))
        var routerStubClass: File? = null

        allDirectories.get().forEach { directory ->
            val directoryUri = directory.asFile.toURI()
            directory.asFile
                .walk()
                .filter { it.isFile }
                .forEach innerEach@{ file ->
                    val filePath = directoryUri
                        .relativize(file.toURI())
                        .path
                        .replace(File.separatorChar, '/')

                    // 检测到插桩类
                    if (filePath == ROUTER_INJECT) {
                        routerStubClass = file
                        return@innerEach
                    }

                    jarOutput.writeEntity(filePath, file.inputStream())

                    if (file.name.endsWith(".class")) {
                        file.inputStream().findClass(handleModels)
                    }
                }
        }

        allJars.get().onEach { file ->
            val jarFile = JarFile(file.asFile)
            jarFile.entries().iterator().forEach { jarEntry ->
                jarOutput.writeEntity(jarEntry.name, jarFile.getInputStream(jarEntry))

                if (jarEntry.name.endsWith(".class")) {
                    jarFile.getInputStream(jarEntry).findClass(handleModels)
                }
            }
            jarFile.close()
        }

        // 修改插桩的类
        // 由于插桩类只在 application 模块下，而 application 模块的 transform 是最后处理的，所以
        // 这里检测到 routerStubClass 时，就相当于已经扫描完了
        routerStubClass?.let {
            println("修改插桩的类")
            jarOutput.putNextEntry(JarEntry(ROUTER_INJECT))
            val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
            val insertVisitor = InsertCodeVisitor(writer, handleModels)
            ClassReader(it.inputStream()).accept(insertVisitor, ClassReader.SKIP_DEBUG)
            jarOutput.write(writer.toByteArray())
            jarOutput.closeEntry()
            println("修改插桩的类 完成")
        }

        jarOutput.close()
    }


    // writeEntity methods check if the file has name that already exists in output jar
    private fun JarOutputStream.writeEntity(name: String, inputStream: InputStream) {
        // check for duplication name first
        if (jarPaths.contains(name)) {
            printDuplicatedMessage(name)
        } else {
            putNextEntry(JarEntry(name))
            inputStream.copyTo(this)
            closeEntry()
            jarPaths.add(name)
        }
    }

    private fun printDuplicatedMessage(name: String) =
        println("Cannot add ${name}, because output Jar already has file with the same name.")
}

fun InputStream.findClass(outHandle: ArrayList<HandleModel>) {
    use { ClassReader(it).accept(FindHandleClass(outHandle), 0) }
}