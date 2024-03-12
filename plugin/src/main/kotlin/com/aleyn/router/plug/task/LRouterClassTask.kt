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
        val time = System.currentTimeMillis()

        val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile)))

        println("jarOutput: ${output.get().asFile}")
        var routerStubClass: File? = null

        allDirectories.get().forEach {
            println("allDirectories: ${it.asFile}")
        }

        allJars.get().forEach {
            println("allJars: ${it.asFile}")
        }

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
//                runCatching {
//                    jarFile.getInputStream(jarEntry).findClass(handleModels)
//                }.onFailure {
//                    println("LRouter handle " + jarEntry.name + " error:${it.message}")
//                }

                /*    try {
                        if (jarEntry.name == ROUTER_INJECT) {
                            waitInsertJar = file.asFile
                            return@forEach
                        }
                        jarOutput.putNextEntry(JarEntry(jarEntry.name))
                        jarFile.getInputStream(jarEntry).use { it.copyTo(jarOutput) }

                        val have = blackList.any { jarEntry.name.startsWith(it) }

                        if (!have && jarEntry.name.endsWith(".class")) {
                            runCatching {
                                jarFile.getInputStream(jarEntry).findClass(handleModels)
                            }.onFailure {
                                println("LRouter handle " + jarEntry.name + " error:${it.message}")
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        jarOutput.closeEntry()
                    }*/
            }
            jarFile.close()
        }

        println("模块：${project.name}，查询到：${handleModels.size}")
        handleModels.forEach {
            println("模块：${project.name} $it")
        }

        // 修改插桩的类
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

        /*       if (waitInsertJar == null) {
                   jarOutput.close()
                   println(":${project.name} -> The class to insert was not found, please check for references LRouter")
                   return
               }
               val jarFile = JarFile(waitInsertJar!!)
               jarOutput.putNextEntry(JarEntry(ROUTER_INJECT))
               jarFile.getInputStream(jarFile.getJarEntry(ROUTER_INJECT)).use {
                   val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                   val insertVisitor = InsertCodeVisitor(writer, handleModels)
                   ClassReader(it).accept(insertVisitor, ClassReader.SKIP_DEBUG)
                   jarOutput.write(writer.toByteArray())
                   jarOutput.closeEntry()
               }
               jarFile.close()*/


        jarOutput.close()

        val spendTime = System.currentTimeMillis() - time
        println("消耗时间：$spendTime ms")
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

    private fun JarOutputStream.writeEntity(relativePath: String, byteArray: ByteArray) {
        // check for duplication name first
        if (jarPaths.contains(relativePath)) {
            printDuplicatedMessage(relativePath)
        } else {
            putNextEntry(JarEntry(relativePath))
            write(byteArray)
            closeEntry()
            jarPaths.add(relativePath)
        }
    }

    private fun printDuplicatedMessage(name: String) =
        println("Cannot add ${name}, because output Jar already has file with the same name.")
}

fun InputStream.findClass(outHandle: ArrayList<HandleModel>) {
    use { ClassReader(it).accept(FindHandleClass(outHandle), 0) }
}