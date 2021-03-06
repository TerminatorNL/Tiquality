# Tiquality
[Tiquality](https://minecraft.curseforge.com/projects/tiquality) is the successor of the seemingly popular mod: LagGoggles.

At this moment, Tiquality is a server side only mod, but supports clients too (Lan hosted).

The core functionality is limiting a player's tick time in the world to evenly distribute time.

## Support
If you have an issue, I'd rather have you submit a new issue report on GitHub. If you think it's not worthy of an issue report on GitHub, there's a [discord channel](https://discord.gg/pM4vwEU) where you can contact me in a more informal way. Please note, issues discussed on GitHub are better because other people can find the same problem, and tune in without digging through hundreds of text messages (More organized) 

## Download
You can download the latest release [here](https://www.curseforge.com/minecraft/mc-mods/tiquality/files/all). The _ONLY_ difference between **THIN** and **FAT** is that **FAT** ships [Mixin](https://github.com/SpongePowered/Mixin), which is a library that Tiquality needs in order to function. It's possible that another mod already ships Mixin or has another version. Generally speaking, Tiquality can handle most Mixin versions well.
A known mod to ship Mixin is **[SpongeForge](https://github.com/SpongePowered/SpongeForge)**. If you have SpongeForge or another mod that requires a different version of Mixin, choose the **THIN** release.

## Modpack authors
Yes, you have my permission! Please add this to your modpack! :thumbsup: It will save server admins a lot of time if you do all the configuration work for them! ([/tq setblock](#commands-and-permissions))

## Configuration
Interested in this mod? Check out the default and documented configuration [here](https://github.com/TerminatorNL/Tiquality/blob/master/Tiquality.cfg).

## What this is, and what it is intended for
An update throttler, that aims to provide a fair Minecraft experience at 20 TPS for users who do not cause a heavy load on the server, whilst players who build carelessly only get 10 TPS (Their TPS gradually decreases as their impact increases).

This encourages server friendly build behavior, and attempts to not ruin gameplay for others who care about their server.

Generally speaking, players won't notice Tiquality's presence, and don't have to interact with it. On rare occasions, [/tq claim](#commands-and-permissions) can help them out instantly.

This isn't a punishment mod, but makes the culprit face the consequences of their actions on their own. This means that everyone else benefits.

## What this is not
This is not something like [ClearLagg](https://dev.bukkit.org/projects/clearlagg). Tiquality tries to keep the game fair for everyone, regardless of what someone else is doing.

## Behavior
Blocks without an owner which are not specified in the config cannot update. This includes freshly generated areas. To keep functionality like leaf decay and grass growth, you can **whitelist** blocks in the config. Whitelisted blocks do not need an owner in order to tick.

If a player places a block, Tiquality will assign that block to that player's personal tracker. This tracker will perform future updates for that block, using the time constraint of the player that placed the block.

If more players log in, the time will be divided more. If a player doesn't use up all of his tick time, other's will receive the remaining time.

Logged out player's blocks have less tick time than players who are online (Config customizable)!

When entities spawn or move between chunks, their tracker is updated to match the most dominant tracker (cached) from that chunk. This means that there's full support for entity throttling as well. The most dominant tracker is generated by selecting the owner with the most blocks in a chunk. You take control of a chunk by building in it automatically.

## Integration
* [SpongeForge](https://github.com/SpongePowered/SpongeForge)
   * Permissions, commands, customized ticking logic
* [GriefPrevention](https://github.com/MinecraftPortCentral/GriefPrevention)
   * Claims automagically set the tracker in the claimed area, and lock it to that owner for as long as that claim is present
* [GriefDefender](https://github.com/bloodmc/GriefDefender)
   * Claims automagically set the tracker in the claimed area, and lock it to that owner for as long as that claim is present 
* [FTB Utilities](https://github.com/FTBTeam/FTB-Utilities)
   * Claims automagically set the tracker in the claimed area, and lock it to that owner for as long as that claim is present (Useful for basemates)
* [LagGoggles](https://github.com/TerminatorNL/LagGoggles) **(Work in progress...)**
   * Interface for not-so-tech-savvy players.

All of the above are optional.

## How it functions
All calls to `Block.randomTickBlock()`, `Block.updateTickBlock()`, `Entity.onUpdate()` and `ITickable.tickTileEntity()` are redirected to Tiquality, which in turn finds the owner of a block or entity using a customized high performance lookup.

There are trackers associated with the block positions of the world and entities, these trackers record how long an update took, and subtract that from the 'granted nanoseconds' until it reaches zero. When it reaches zero all future updates are queued for later, and the update doesn't execute right away.

When the next tick comes around, all trackers get a granted amount of tracking time, meaning blocks update again. The queue is executed first. If the queue doesn't complete, new updates will be added to the queue if it isn't already.

If a player isn't using up all his tick time, the remaining time will be consumed by players who who have used up all their time.

You can share tick time with other players using [/tq share](#commands-and-permissions). This is useful for basemates.

## Commands and permissions
 * /tiquality
   * Main command
   * tiquality.use
   
 * /tq
   * Alias for /tiquality
   * tiquality.use
   
 * /tq info [`point`]
   * Prints info about the block you're standing on and the block your feet are inside (Liquids).<br>Prints the owner of the block(s)<br>Prints the current status if a block is on the whitelist or not.<br>If used with the 'point' flag, you can aim at blocks and sneak for quick info on who tracked a block.
   * tiquality.use
  
 * /tq share &lt;`playername`&gt;
   * Allows sharing tick time with basemates, meaning that you can keep playing if the base owner is offline. If both of you are online, the tick time is effectively doubled.
   * tiquality.use
  
 * /tq track
   * Claims a block for a player without actually having to re-place it. (Things found in nature, existing bases... etc)
   * tiquality.use
   
 * /tq claim [`radius`]
   * Claims an area for a player. The maximum radius is defined using `MAX_CLAIM_RADIUS` in the config. If the radius parameter is omitted, the maximum value in the config is used instead. This is especially useful if you have just installed Tiquality and need to import bases into your existing world.
   * tiquality.claim
   
 * /tq acceptoverride <`player`>
   * Allows another player to claim your area after a failed claim attempt. You will receive a message on when to use this.
   * tiquality.use
   
 * /tq denyoverride <`player`>
   * Denies another player to claim your area after a failed claim attempt. You will receive a message on when to use this.
   * tiquality.use
   
* /tq unclaim [`radius`]
  * Unclaims an area, useful for admins.
  * tiquality.admin
   
 * /tq profile &lt;`seconds`&gt;
   * Runs a very basic profiler on blocks that you own. A better alternative is in the works.
   * tiquality.use
  
 * /tq profile &lt;`seconds`&gt; &lt;`uuid` or `playername`&gt;
   * Runs a very basic profiler on the targeted UUID or playername. A better alternative is in the works.
   * tiquality.admin
 
 * /tq setblock &lt;`feet`|`below`&gt; &lt;`DEFAULT`|`NATURAL`|`PRIORITY`|`ALWAYS_TICK`|`TICK_DENIED`&gt;
   * Sets all the blocks of the specified type to change tick behavior.<br>
   Valid types are:<br>
     * `DEFAULT`: 
       * Only ticks when a tracker is assigned AND has time to tick. Can be throttled
     * `PRIORITY`: 
       * Like `DEFAULT`, but ticks before everything else in the same tracker. Can be throttled
     * `TICK_DENIED`:
       * Never ticks
     * `NATURAL`: Ticks when either:
       * Ticks when *no tracker* is assigned. When a tracker has been assigned, it can be throttled
     * `ALWAYS_TICK`:
       * Always ticks, never throttled. If a tracker has been assigned, it will still affect the granted time for a tracker.
   * tiquality.admin
   
 * /tq setentity &lt;`entity_name`&gt; &lt;`DEFAULT`|`NATURAL`|`PRIORITY`|`ALWAYS_TICK`|`TICK_DENIED`&gt;
   * Sets all the entities of the specified type to change tick behavior.<br>
    Valid types are:<br>
      * `DEFAULT`: 
        * Ticks when no tracker is assigned. When a tracker has been assigned, it can be throttled if no time is left.
      * `PRIORITY`: 
        * Like `DEFAULT`, but ticks before everything else in the same tracker. Can be throttled
      * `TICK_DENIED`:
        * Never ticks
      * `NATURAL`: Ticks when either:
        * Exactly the same as `DEFAULT`
      * `ALWAYS_TICK`:
        * Always ticks, never throttled. If a tracker has been assigned, it will still affect the granted time for a tracker.
    * tiquality.admin
  
 * /tq reload
   * Reloads the config file.
   * tiquality.admin
   
   

## Frequently asked questions
 
- [Why don't you move to Sponge already!?](#why-dont-you-move-to-sponge-already)
- [What does `/tq info` do?](#what-does-tq-info-do)
- [My blocks don't tick! What do I do?](#my-blocks-dont-tick-what-do-i-do)
- [My fluids don't flow! What do I do?](#my-fluids-dont-flow-what-do-i-do)
- [What is your code style?](#what-is-your-code-style)
- [I just installed Tiquality, and the TPS is HORRIBLE!](#i-just-installed-tiquality-and-the-tps-is-horrible)
- [Why store data in the chunk itself?](#where-is-the-data-stored)
- [You suck!](#you-suck)

### Why don't you move to Sponge already!?
It is my intention to make Tiquality as widely available to everyone. Not having to install Sponge, match the Forge version, find mods that are both compatible with Sponge and that specific Forge version makes it easier to install. Everyone should be able to use Tiquality, *even if you run a Sponge-free server.*

**Sponge is still supported, however.**

### What does `/tq info` do?
It helps you diagnose if a block can be ticked or not, as well as finding names to use in the config

Stand on top of the block and use `/tq info`. The output will be as follows:
```
No entities are found in your chunk
Block below: minecraft:piston TickType: DEFAULT Status: Tracked by: Terminator_NL
```
We can break this down:
 - "minecraft:piston" is the block name
 - "TickType: DEFAULT" means that this block will only tick if a tracker has been assigned and has time to spare. (See: [/tq track](#commands-and-permissions))
 - "Tracked by: XXX" tells us if a block is tracked or not, in this case: It is tracked by me.
 
Another example of `tq info`:
```
Entities in chunk:
minecraft:item PRIORITY Not tracked
Block below: minecraft:grass TickType: NATURAL Status: Not tracked
```
We can break this down:
 - "minecraft:item" is the entity name of a dropped item in the chunk
 - "minecraft:grass" is the block name
 - "TickType: NATURAL" means that this block **type** will tick if it's not tracked or the tracker has time to spare. (See: [/tq setblock](#commands-and-permissions))
 - "Not tracked" tells us that no tracker has been assigned to this block.

Recommended usage:
 - `tq info point` which allows you to see what blocks are tracked simply by looking at them, and sneaking.
 - `tq info` to see if your config changes worked, or helps players diagnose server configuration issues.

### My blocks don't tick! What do I do?

A block will tick if at least one of the following statements is true:
 - There's a Tracker assigned and the tracker has enough time to tick the block
 - The block is defined in the config (`NATURAL_BLOCKS`) and there's no tracker assigned to it
 - The block is defined in the config (`NATURAL_BLOCKS`) and the tracker has enough time to tick the block
 - The block is defined in the config (`ALWAYS_TICKED_BLOCKS`) It will tick even if a tracker has been assigned that ran out of time. Note that this will still consume the time on the tracker.
 
The fastest way to solve this is simply by standing on the block and running `/tq setblock below NATURAL`. It will add the block to the config under `NATURAL_BLOCKS`.

Pro tip: Use [`/tq info`](#what-does-tq-info-do) first, to see if you are actually positioned on the block correctly.

### My fluids don't flow! What do I do?

Fluid's are tracked the same way as [blocks](#my-blocks-dont-tick-what-do-i-do).


The fastest way to solve this is simply by standing in the liquid and running `/tq setblock feet NATURAL`. It will add the fluid to the config under `NATURAL_BLOCKS`.

Pro tip: Use [`/tq info`](#what-does-tq-info-do) first, to see if you are actually positioned in the liquid correctly.


### I just installed Tiquality, and the TPS is HORRIBLE!
Keep in mind, that there are MANY other reasons the TPS can still not be 20. Chunkloading unloaded chunks is one of the big effectors: people exploring, or logging in. Another big effector is post-world-tick processing.
Tiquality does **NOT** hook into world events, so any processing AFTER the world tick is completely outside of Tiquality's control. You can try to raise the config value `TIME_BETWEEN_TICKS_IN_NS`. Remember that increasing this value will cause the world to stop ticking sooner, effectively making the world tick slower. 


### Where is the data stored?
The data is stored in the world folder under `TiqualityStorage`. Inspired by minecraft's own code, Tiquality also uses bitshifting to find the right identifier for a block, without having to iterate on anything. This means blazing fast performance, and this is needed because Tiquality has to **intercept and act on every ticked object**.

### I have an idea! What can I do to help?
Make an issue at [GitHub](https://github.com/TerminatorNL/Tiquality/issues/) and I'll give you some feedback. (I might already have considered your idea!) After that, fork Tiquality and just make a pull request!
If you can't code, you can obviously still submit requests, and I will implement it as long as it fits the scope of the project.
