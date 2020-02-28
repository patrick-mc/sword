package com.github.patrick.sword

class SwordTask : Runnable {
    override fun run() {
        SwordProcess.swordPlayer.values.forEach {
            it.swords.forEachIndexed { i, e -> e.update(i / it.swords.count().toDouble()) }
            it.flyingSword.forEach { entity -> entity.update(0.0) }
            it.flyingSword.removeIf { entity -> !entity.valid }
        }
    }
}