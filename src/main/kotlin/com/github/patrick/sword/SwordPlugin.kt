package com.github.patrick.sword

import com.github.patrick.sword.SwordProcess.start
import com.github.patrick.sword.SwordProcess.stop
import org.bukkit.plugin.java.JavaPlugin

@Suppress("UNUSED")
internal class SwordPlugin : JavaPlugin() {
    override fun onEnable() = start(this)

    override fun onDisable() = stop()
}