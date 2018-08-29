# Tiquality
Tiquality is the successor of the seemingly popular mod: LagGoggles.

At this moment, Tiquality is a serverside only mod.

The core functionality is limiting a player's tick time in the world to evenly distribute time.

## What this is, and what it is intended for
An update throttler, that aims to provide a fair Minecraft experience at 20 TPS for users who do not cause a heavy load on the server, whilst players who build carelessly only get 10 TPS (depending on their impact).

This encourages server friendly build behavior, and attempts to not ruin gameplay for others who care about their server.

## What this is not
 * This is not something like [ClearLagg](https://dev.bukkit.org/projects/clearlagg). Entities like sheep, dropped items, etc are NOT limited or removed. Tiquality tries to keep the game fair for everyone, regardless of what someone else is doing.
 * This will not fix lag caused by huge mobfarms because of too many entities, entities (except TileEntities) are left untouched.


## Behavior
Blocks without an owner which are not specified in the config cannot update. This includes freshly generated areas. To keep functionality like leaf decay and grass growth, you can **whitelist** blocks in the config. Whitelisted blocks do not need an owner in order to tick.

If a player places a block, Tiquality will assign that block to that player's personal tracker. This tracker will perform future updates for that block, using the time constraint of the player that placed the block.

If more players log in, the time will be divided more. If a player doesn't use up all of his tick time, other's will receive the remaining time.

Logged out player's blocks have less tick time than players who are online (Config customizable)!


## How it functions:
All calls to `Block.randomTickBlock()`, `Block.updateTickBlock()` and `ITickable.tickTileEntity()` are redirected to Tiquality, which in turn finds the owner of a block using a customized high performance lookup.

There are trackers associated with the block positions of the world, these trackers record how long an update took, and substract that from the 'granted nanoseconds' until it reaches zero. When it reaches zero all future updates are queued for later, and doesn't tick right away.

When the next tick comes around, all trackers get a granted amount of tracking time, meaning blocks update again. The queue is executed first. If the queue doesn't complete, new updates will be added to the queue if it isn't already.

## Commands and permissions
 * /tiquality
   * Main command
   * tiquality.use
   
 * /tq
   * Alias for /tiquality
   * tiquality.use
   
 * /tq info
   * Prints info about the block you're standing on and the block your feet are inside (Liquids).<br>Prints the owner of the block(s)<br>Prints the current status if a block is on the whitelist or not.
   * tiquality.use
  
 * /tq claim
   * Claims a block for a player without actually having to re-place it. (Things found in nature, existing bases... etc)
   * tiquality.use
```diff
- CLAIMING IS NOT IMPLEMENTED YET!
```
 
 * /tq add <feet|below>
   * Adds a block to the whitelist, making blocks of that type tick without owners.
   * tiquality.admin
  
 * /tq reload
   * Reloads the config file.
   * tiquality.admin
