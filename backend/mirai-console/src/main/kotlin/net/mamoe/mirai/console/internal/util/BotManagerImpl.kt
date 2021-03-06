/*
 * Copyright 2019-2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AFFERO GENERAL PUBLIC LICENSE version 3 license that can be found via the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package net.mamoe.mirai.console.internal.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.internal.MiraiConsoleImplementationBridge
import net.mamoe.mirai.console.util.BotManager
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.utils.minutesToMillis
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal object BotManagerImpl : BotManager {
    /**
     * 判断此用户是否为 console 管理员
     */
    override val User.isManager: Boolean get() = this.id in ManagersConfig[this.bot]

    override fun Bot.removeManager(id: Long): Boolean {
        return ManagersConfig[this].remove(id)
    }

    override val Bot.managers: List<Long>
        get() = ManagersConfig[this].toList()

    override fun Bot.addManager(id: Long): Boolean {
        return ManagersConfig[this].add(id)
    }
}

internal object ManagersConfig : AutoSavePluginConfig() {
    private val managers by value<MutableMap<Long, MutableSet<Long>>>().withEmptyDefault()
    internal operator fun get(bot: Bot): MutableSet<Long> = managers[bot.id]!!
}


internal fun CoroutineContext.overrideWithSupervisorJob(): CoroutineContext = this + SupervisorJob(this[Job])
internal fun CoroutineScope.childScope(context: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    CoroutineScope(this.childScopeContext(context))

internal fun CoroutineScope.childScopeContext(context: CoroutineContext = EmptyCoroutineContext): CoroutineContext =
    this.coroutineContext.overrideWithSupervisorJob() + context

internal object ConsoleDataScope : CoroutineScope by MiraiConsole.childScope() {
    private val data: Array<out PluginData> = arrayOf()
    private val configs: Array<out PluginConfig> = arrayOf(ManagersConfig)

    fun reloadAll() {
        data.forEach { dt ->
            ConsoleBuiltInPluginDataStorage.load(ConsoleBuiltInPluginDataHolder, dt)
        }
        configs.forEach { config ->
            ConsoleBuiltInPluginConfigStorage.load(ConsoleBuiltInPluginConfigHolder, config)
        }
    }
}

internal object ConsoleBuiltInPluginDataHolder : AutoSavePluginDataHolder,
    CoroutineScope by ConsoleDataScope.childScope() {
    override val autoSaveIntervalMillis: LongRange = 1.minutesToMillis..10.minutesToMillis
    override val name: String get() = "ConsoleBuiltIns"
}

internal object ConsoleBuiltInPluginConfigHolder : AutoSavePluginDataHolder,
    CoroutineScope by ConsoleDataScope.childScope() {
    override val autoSaveIntervalMillis: LongRange = 1.minutesToMillis..10.minutesToMillis
    override val name: String get() = "ConsoleBuiltIns"
}

internal object ConsoleBuiltInPluginDataStorage :
    PluginDataStorage by MiraiConsoleImplementationBridge.dataStorageForBuiltIns

internal object ConsoleBuiltInPluginConfigStorage :
    PluginDataStorage by MiraiConsoleImplementationBridge.configStorageForBuiltIns