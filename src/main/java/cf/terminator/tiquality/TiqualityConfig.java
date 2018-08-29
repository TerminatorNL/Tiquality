package cf.terminator.tiquality;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

@Config(modid = Tiquality.MODID, name = Tiquality.NAME, type = Config.Type.INSTANCE, category = "TickThrottling")
public class TiqualityConfig {

    @Config.Comment({
            "TiqualityCommand pre-allocates the max tick time someone can use.",
            "This includes offline players (Loaded chunks with an offline player's base, for example)",
            "With this in mind, what multiplier should we use to assign tick time to them?",
            "",
            "Where 0 means offline player's base does not get any pre-allocated tick time and 1 means they will get the same tick time as an online player.",
            "Keep in mind that people might be living together..."
    })
    @Config.RangeDouble(min = 0, max = 1)
    public static double OFFLINE_PLAYER_TICK_TIME_MULTIPLIER = 0.5;

    @Config.Comment({
            "How much tick time should be assigned to world objects defined in the AUTO_WORLD_ASSIGNED_OBJECTS variable?"
    })
    @Config.RangeDouble(min = 0, max = 1)
    public static double WORLD_PLAYER_TICK_TIME_MULTIPLIER = 0.1;

    @Config.Comment({
            "Some blocks are automatically generated in the world, but do require ticking in order to funtion properly.",
            "Define the blocks you wish to keep tick using the WORLD_PLAYER when the block has not been assigned an owner yet.",
    })
    public static String[] AUTO_WORLD_ASSIGNED_OBJECTS = new String[]{
            "minecraft:mob_spawner",
            "minecraft:chest",
            "minecraft:ender_chest",
            "minecraft:trapped_chest",
            "REGEX=leaves",
            "REGEX=sapling",
            "REGEX=flowing",
            "minecraft:snow_layer",
            "minecraft:ice",
            "minecraft:water",
            "minecraft:lava",
            "minecraft:grass",
            "minecraft:sand",
            "minecraft:gravel",
            "minecraft:beetroots",
            "minecraft:wheat",
            "minecraft:carrots",
            "minecraft:potatoes",
            "minecraft:reeds",
            "minecraft:farmland",
            "minecraft:fire",
            "minecraft:cocoa",
            "minecraft:cactus",
            "minecraft:double_plant"
    };

    @Config.Comment({
            "Between ticks, the server must do some internal processing.",
            "Increase this value if you see \"can't keep up!\" errors.",
            "Try to keep this value as low as possible for performance."
    })
    @Config.RangeInt(min = 0)
    public static int TIME_BETWEEN_TICKS_IN_NS = 90000;

    public static class QuickConfig{

        public static HashSet<Block> AUTO_WORLD_ASSIGNED_OBJECTS_FAST = new HashSet<>();

        public static void saveToFile(){
            ConfigManager.sync(Tiquality.MODID, Config.Type.INSTANCE);
        }

        public static void reloadFromFile() {
            Tiquality.LOGGER.info("SCANNING BLOCKS...");

            /* I know this is not a proper way to do this, if you know of a better way to reload the config
             * FROM DISK, I'd gladly move to your solution.
             */
            try {
                Field field = ConfigManager.class.getDeclaredField("CONFIGS");
                field.setAccessible(true);
                @SuppressWarnings("unchecked") Map<String, Configuration> STOLEN_VARIABLE = (Map<String, Configuration>) field.get(null);
                STOLEN_VARIABLE.remove(new File(Loader.instance().getConfigDir(), "Tiquality.cfg").getAbsolutePath());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            saveToFile();
        }

        public static void update(){
            AUTO_WORLD_ASSIGNED_OBJECTS_FAST.clear();

            for (String input : AUTO_WORLD_ASSIGNED_OBJECTS) {
                if(input.startsWith("REGEX=")){
                    AUTO_WORLD_ASSIGNED_OBJECTS_FAST.addAll(findBlocks(input.substring(6,input.length())));
                }else {
                    String[] split = input.split(":");
                    ResourceLocation location = new ResourceLocation(split[0], split[1]);

                    Block block = Block.REGISTRY.getObject(location);

                    if (block == Blocks.AIR) {
                        Tiquality.LOGGER.warn("!!!!#######################!!!!");
                        Tiquality.LOGGER.warn("INVALID CONFIG ENTRY");
                        Tiquality.LOGGER.warn("AUTO_WORLD_ASSIGNED_OBJECTS: " + block);
                        Tiquality.LOGGER.warn("This block has been skipped!");
                        Tiquality.LOGGER.warn("!!!!#######################!!!!");
                        continue;
                    }
                    AUTO_WORLD_ASSIGNED_OBJECTS_FAST.add(block);
                }
            }
            for(Block b : AUTO_WORLD_ASSIGNED_OBJECTS_FAST){
                Tiquality.LOGGER.info("+ " + Block.REGISTRY.getNameForObject(b).toString());
            }
        }

        private static ArrayList<Block> findBlocks(String regex){
            ArrayList<Block> list = new ArrayList<>();
            for(ResourceLocation resource : Block.REGISTRY.getKeys()){
                if(Pattern.compile(regex).matcher(resource.toString()).find()){
                    list.add(Block.REGISTRY.getObject(resource));
                    Tiquality.LOGGER.info("r '" + regex + "' applied for: " + resource.toString());
                }
            }
            if(list.size() == 0){
                Tiquality.LOGGER.warn("r '" + regex + "' had no matches!");
            }
            return list;
        }
    }
}
