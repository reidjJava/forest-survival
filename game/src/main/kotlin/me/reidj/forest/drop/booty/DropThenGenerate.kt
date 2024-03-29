package me.reidj.forest.drop.booty

import me.reidj.forest.channel.ModHelper
import me.reidj.forest.drop.block.BlockUnit
import me.reidj.forest.user.User
import org.bukkit.Location
import org.bukkit.Material

object DropThenGenerate : Booty {
    override fun get(resource: BlockUnit, location: Location, user: User) {
        resource.drop(location, user.player!!)
        resource.generate(location)
        location.block.type = Material.AIR
        ModHelper.highlight(user.player!!)
    }
}