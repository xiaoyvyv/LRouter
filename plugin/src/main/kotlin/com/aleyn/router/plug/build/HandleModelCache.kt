package com.aleyn.router.plug.build

import com.aleyn.router.plug.data.HandleModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile

/**
 * 这里需要按模块缓存读取的 HandleModel
 */
object HandleModelCache {
    private val gson by lazy { Gson() }

    private val Project.routerJsonDir: Directory
        get() = project.layout.buildDirectory.dir("intermediates/router/out").get()
            .apply { asFile.mkdirs() }

    private inline fun <reified T> RegularFile.writeModel(data: T) {
        val file = asFile
        if (file.exists()) file.delete()
        file.createNewFile()
        file.writeText(gson.toJson(data))
    }

    private inline fun <reified T> RegularFile.readModel(): T? {
        return runCatching {
            val type = object : TypeToken<T>() {}.type
            gson.fromJson<T>(asFile.readText(), type)
        }.getOrNull()
    }

    fun saveHandleModel(project: Project, models: List<HandleModel>) {
        val directory = project.routerJsonDir

        val modules = models.filterIsInstance<HandleModel.Module>()
        val autowired = models.filterIsInstance<HandleModel.Autowired>()
        val intercepts = models.filterIsInstance<HandleModel.Intercept>()
        val initializers = models.filterIsInstance<HandleModel.Initializer>()

        directory.file("modules.json").writeModel(modules)
        directory.file("autowired.json").writeModel(autowired)
        directory.file("intercepts.json").writeModel(intercepts)
        directory.file("initializers.json").writeModel(initializers)
    }

    fun readHandleModel(rootProject: Project): List<HandleModel> {
        val models = mutableListOf<HandleModel>()

        rootProject.subprojects {
            val buildDirectory = it.routerJsonDir

            val modules = buildDirectory.file("modules.json")
                .readModel<List<HandleModel.Module>>()
                .orEmpty()
            val autowired = buildDirectory.file("autowired.json")
                .readModel<List<HandleModel.Autowired>>()
                .orEmpty()
            val intercepts = buildDirectory.file("intercepts.json")
                .readModel<List<HandleModel.Intercept>>()
                .orEmpty()
            val initializers = buildDirectory.file("initializers.json")
                .readModel<List<HandleModel.Initializer>>()
                .orEmpty()

            models.addAll(modules)
            models.addAll(autowired)
            models.addAll(intercepts)
            models.addAll(initializers)
        }
        return models
    }
}
