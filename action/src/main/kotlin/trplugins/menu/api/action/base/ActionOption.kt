package trplugins.menu.api.action.base

import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.onlinePlayers
import taboolib.common.util.random
import trplugins.menu.api.action.Actions.Companion.scriptParser
import java.util.function.Consumer

/**
 * @author Arasple
 * @date 2021/1/29 17:56
 */
class ActionOption(val set: Map<Type, String> = mapOf()) {

    fun evalChance(): Boolean {
        return if (!set.containsKey(Type.CHANCE)) true else random(set[Type.CHANCE]!!.toDoubleOrNull() ?: 0.0)
    }

    fun evalDelay(): Long {
        return if (!set.containsKey(Type.DELAY)) -1L else set[Type.DELAY]!!.toLongOrNull() ?: -1L
    }

    fun evalPlayers(defPlayer: ProxyPlayer, action: Consumer<ProxyPlayer>) =
        evalPlayers(defPlayer) { action.accept(it) }

    fun evalPlayers(defPlayer: ProxyPlayer, action: (ProxyPlayer) -> Unit) {
        if (set.containsKey(Type.PLAYERS)) {
            val cond = set[Type.PLAYERS].toString()
            onlinePlayers().forEach {
                if (cond.isBlank()) action(it)
                else if (scriptParser.parse(it, cond).asBoolean(false)) action(it)
            }
        } else {
            action.invoke(defPlayer)
        }
    }

    fun evalCondition(player: ProxyPlayer): Boolean {
        return if (!set.containsKey(Type.CONDITION)) {
            true
        } else {
            set[Type.CONDITION]?.let {
                scriptParser.parse(player, it).asBoolean()
            } ?: false
        }
    }

    companion object {

        fun of(string: String): Pair<String, ActionOption> {
            var content = string
            val options = mutableMapOf<Type, String>()

            Type.values().forEach {
                it.regex.find(content)?.let { find ->
                    val value = find.groupValues.getOrElse(it.group) { "" }
                    options[it] = value
                    content = it.regex.replace(content, "")
                }
            }

            return content.removePrefix(" ") to ActionOption(options)
        }

    }

    enum class Type(val regex: Regex, val group: Int) {

        DELAY("[{<](delay|wait)[=:] ?([0-9]+)[}>]", 2),

        CHANCE("[{<](chance|rate|rand(om)?)[=:] ?([0-9.]+)[}>]", 3),

        CONDITION("[{<](condition|requirement)[=:] ?(.+)[}>]", 2),

        PLAYERS("[{<]players[=:]? ?(.*)[}>]", 1);

        constructor(regex: String, group: Int) : this("(?i)$regex".toRegex(), group)

    }

}
