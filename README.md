# Tiquality
[Tiquality](https://minecraft.curseforge.com/projects/tiquality) is the successor of the seemingly popular mod: LagGoggles.

At this moment, Tiquality is a server side only mod, but supports clients too (Lan hosted).

The core functionality is limiting a player's tick time in the world to evenly distribute time.

## Download
You can download the latest release [here](https://minecraft.curseforge.com/projects/tiquality/files)

## Modpack authors
Yes, you have my permission! Please add this to your modpack! :thumbsup: It will save server admins alot of time if you do all the configuration work for them! ([/tq set](#commands-and-permissions))

## What this is, and what it is intended for
An update throttler, that aims to provide a fair Minecraft experience at 20 TPS for users who do not cause a heavy load on the server, whilst players who build carelessly only get 10 TPS (depending on their impact).

This encourages server friendly build behavior, and attempts to not ruin gameplay for others who care about their server.

## What this is not
 * This is not something like [ClearLagg](https://dev.bukkit.org/projects/clearlagg). Tiquality tries to keep the game fair for everyone, regardless of what someone else is doing.

## Behavior
Blocks without an owner which are not specified in the config cannot update. This includes freshly generated areas. To keep functionality like leaf decay and grass growth, you can **whitelist** blocks in the config. Whitelisted blocks do not need an owner in order to tick.

If a player places a block, Tiquality will assign that block to that player's personal tracker. This tracker will perform future updates for that block, using the time constraint of the player that placed the block.

If more players log in, the time will be divided more. If a player doesn't use up all of his tick time, other's will receive the remaining time.

Logged out player's blocks have less tick time than players who are online (Config customizable)!

## Integration
* [GriefPrevention](https://github.com/MinecraftPortCentral/GriefPrevention)
* [FTB Utilities](https://github.com/FTBTeam/FTB-Utilities)

All of the above are optional.

If you have either [GriefPrevention](https://github.com/MinecraftPortCentral/GriefPrevention) or [FTB Utilities](https://github.com/FTBTeam/FTB-Utilities) installed, entities inside claimed land are also tick throttled. There's a config option to make dropped items tick faster by putting them in front of the queue `UPDATE_ITEMS_FIRST`. Players are **never** throttled.



## WARNING
Removing Tiquality will **PERMANENTLY** remove it's existing data on chunk load on a per-chunk basis.

If you are looking to permanently delete Tiquality, simply start the server without it, and all data will get lost over time.

## How it functions
All calls to `Block.randomTickBlock()`, `Block.updateTickBlock()`, `Entity.onUpdate()` and `ITickable.tickTileEntity()` are redirected to Tiquality, which in turn finds the owner of a block using a customized high performance lookup.

There are trackers associated with the block positions of the world, these trackers record how long an update took, and subtract that from the 'granted nanoseconds' until it reaches zero. When it reaches zero all future updates are queued for later, and doesn't tick right away.

When the next tick comes around, all trackers get a granted amount of tracking time, meaning blocks update again. The queue is executed first. If the queue doesn't complete, new updates will be added to the queue if it isn't already.

## Commands and permissions
 * /tiquality
   * Main command
   * tiquality.use
   
 * /tq
   * Alias for /tiquality
   * tiquality.use
   
 * /tq info [point]
   * Prints info about the block you're standing on and the block your feet are inside (Liquids).<br>Prints the owner of the block(s)<br>Prints the current status if a block is on the whitelist or not.<br>If used with the 'point' flag, you can aim at blocks and sneak for quick info on who tracked a block.
   * tiquality.use
  
 * /tq share &lt;playername&gt;
   * Allows sharing tick time with basemates, meaning that you can keep playing if the base owner is offline. If both of you are online, the tick time is effectively doubled.
   * tiquality.use
  
 * /tq track
   * Claims a block for a player without actually having to re-place it. (Things found in nature, existing bases... etc)
   * tiquality.use
   
 * /tq claim [radius]
   * Claims an area for a player. The maximum radius is defined using `MAX_CLAIM_RADIUS` in the config. If the radius parameter is omitted, the maximum value in the config is used instead. This is especially useful if you have just installed Tiquality and need to import bases into your existing world.
   * tiquality.claim
   
* /tq unclaim [radius]
  * Unclaims an area, useful for admins.
  * tiquality.admin
   
 * /tq profile &lt;seconds&gt;
   * Runs a very basic profiler on blocks that you own. A better alternative is in the works.
   * tiquality.use
  
 * /tq profile &lt;seconds&gt; &lt;uuid or playername&gt;
   * Runs a very basic profiler on the targeted UUID or playername. A better alternative is in the works.
   * tiquality.admin
 
 * /tq set &lt;feet|below&gt; &lt;DEFAULT|NATURAL|ALWAYS_TICK&gt;
   * Sets all the blocks of the specified type to change tick behavior.<br>
   Valid types are:<br>
     * `DEFAULT`: Only ticks when a tracker has been assigned AND there's tick time left.
     * `NATURAL`: Ticks when either:
       * No tracker is assigned
       * A tracker is assigned AND there's tick time left.
     * `ALWAYS_TICK`: always ticks, does not check for trackers.
   * tiquality.admin
  
 * /tq reload
   * Reloads the config file.
   * tiquality.admin
   
 * /tq import_griefprevention
   * Will update every GriefPrevention claim existing before Tiquality was installed with TiqualityTrackers. You only need to run this once during first run! This command is hidden from TAB autocompletion.
   * tiquality.admin
   
 * /tq import_griefprevention_claim
   * Will update the GriefPrevention claim (where you are currently standing) existing before Tiquality was installed with TiqualityTrackers. You must use this command if the command above loaded a corrupt chunk you do not intend to fix. You only need to run this once for each claim that existed before Tiquality was installed! This command is hidden from TAB autocompletion. Abuse is prevented by only updating if no tracker yet exists.
   * tiquality.use

## Frequently asked questions
 
- [Why don't you move to Sponge already!?](#why-dont-you-move-to-sponge-already)
- [What does `/tq info` do?](#what-does-tq-info-do)
- [My blocks don't tick! What do I do?](#my-blocks-dont-tick-what-do-i-do)
- [My fluids don't flow! What do I do?](#my-fluids-dont-flow-what-do-i-do)
- [What is your code style?](#what-is-your-code-style)
- [I just installed Tiquality, and the TPS is HORRIBLE!](#i-just-installed-tiquality-and-the-tps-is-horrible)
- [Why store data in the chunk itself?](#why-store-data-in-the-chunk-itself)
- [You suck!](#you-suck)

### Why don't you move to Sponge already!?
It is my intention to make Tiquality as widely available to everyone. Not having to install Sponge, match the Forge version, find mods that are both compatible with Sponge and that specific Forge version makes it easier to install. Everyone should be able to use Tiquality, *even if you run a Sponge-free server.*

**Sponge is still supported, however.**

### What does `/tq info` do?
It helps you diagnose if a block can be ticked or not

Stand on top of the block and use `/tq info`. The output will be as follows:
```
Block below: minecraft:piston TickType: DEFAULT Status: Tracked by: Terminator_NL
```
We can break this down:
 - "minecraft:piston" is the block name
 - "TickType: DEFAULT" means that this block will only tick if a tracker has been assigned and has time to spare. (See: [/tq track](#commands-and-permissions))
 - "Tracked by: XXX" tells us if a block is tracked or not, in this case: It is tracked by me.
 
Another example of `tq info`:
```
Block below: minecraft:sand TickType: NATURAL Status: Not tracked
```
We can break this down:
 - "minecraft:sand" is the block name
 - "TickType: NATURAL" means that this block **type** will tick if it's not tracked or the tracker has time to spare. (See: [/tq set](#commands-and-permissions))
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
 
The fastest way to solve this is simply by standing on the block and running `/tq set below NATURAL`. It will add the block to the config under `NATURAL_BLOCKS`.

Pro tip: Use [`/tq info`](#what-does-tq-info-do) first, to see if you are actually positioned on the block correctly.

### My fluids don't flow! What do I do?

Fluid's are tracked the same way as [blocks](#my-blocks-dont-tick-what-do-i-do).


The fastest way to solve this is simply by standing in the liquid and running `/tq set feet NATURAL`. It will add the fluid to the config under `NATURAL_BLOCKS`.

Pro tip: Use [`/tq info`](#what-does-tq-info-do) first, to see if you are actually positioned in the liquid correctly.


### I just installed Tiquality, and the TPS is HORRIBLE!
Keep in mind, that there are MANY other reasons the TPS can still not be 20. Chunkloading unloaded chunks is one of the big effectors: people exploring, or logging in. Another big effector is post-world-tick processing.
Tiquality does **NOT** hook into world events, so any processing AFTER the world tick is completely outside of Tiquality's control. You can try to raise the config value `TIME_BETWEEN_TICKS_IN_NS`. Remember that increasing this value will cause the world to stop ticking sooner, effectively making the world tick slower. 


### Why store data in the chunk itself?
Storing data in the chunk allows for easier resets and makes sure the data does not go out of sync with the chunk.
Inspired by minecraft's own code, Tiquality also uses bitshifting to find the right identifier for a block, without having to iterate on anything. This means blazing fast performance, and this is needed because Tiquality has to **intercept and act on every ticked object**.

### You suck!
Hey, I am just trying to make the world a better place, I am sorry it did not work out for you.
At the time of writing I have had several encounters where people flat out accuse me of doing everything wrong, and simply put: It hurts, lets not do this?

### What is your code style?

I follow my own set of rules whilst coding, to keep intent as clear as possible.
 - `== true` clarifies intent
 - `== false` is much more visible than `!`
 - There's no shorthands like `if()return;` I **always** use blocks
  ```
  if(){
     return;
  }
  ```
 - There are no lambdas, simply because I don't like them for readability purposes (Subject to change)
 - AtomicInteger can double as object for synchronization signaling when waiting for *N* tasks to end, instead of sleeping for a set time.

If you feel like you found something that needs to change, please follow the rules above before submitting a pull request.
Another note: Please notify me beforehand if you intent to drop a big pull request, so I can give you some feedback if it will make it in the master branch before you waste a lot of time on something I already considered.