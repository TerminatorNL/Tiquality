package cf.terminator.tiquality.mixinhelper;

import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MixinConfigPlugin implements IMixinConfigPlugin {

    public static boolean spongePresent = false;
    public static boolean hasClientClasses = true;
    public static boolean griefPreventionPresent = false;
    public static boolean MIXIN_CONFIG_PLUGIN_WAS_LOADED = false;
    private static final Logger LOGGER = LogManager.getLogger("Tiquality-Mixin");

    /*
     * Snippet straight out of SpongForge's source.
     */
    public static boolean isProductionEnvironment(){
        return System.getProperty("net.minecraftforge.gradle.GradleStart.csvDir") == null;
    }

    private void setup(){
        if(MIXIN_CONFIG_PLUGIN_WAS_LOADED == false){
            MIXIN_CONFIG_PLUGIN_WAS_LOADED = true;
            if(getClass().getClassLoader().getResource("net/minecraft/client/main/Main.class") == null){
                hasClientClasses = false;
                LOGGER.info("Loading server classes");
            }else{
                hasClientClasses = true;
                LOGGER.info("Loading client classes");
            }
            try {
                Class.forName("org.spongepowered.mod.SpongeCoremod", false, getClass().getClassLoader());
                LOGGER.info("Sponge is present!");
                spongePresent = true;
            } catch (ClassNotFoundException ignored) {
                LOGGER.info("Sponge is not present!");
                spongePresent = false;
                try{
                    Class.forName("org.spongepowered.asm.launch.MixinTweaker", false, getClass().getClassLoader());
                } catch (ClassNotFoundException ignored_1) {
                    LOGGER.info("Oh no! It looks like you also do not have Mixin installed. Please use the FAT version of Tiquality.");
                    FMLCommonHandler.instance().exitJava(1, true);
                }
            }

            if(isProductionEnvironment()){
                LOGGER.info("We're running in a production environment.");
                CodeSource source = getClass().getProtectionDomain().getCodeSource();
                String path = source.getLocation().getFile().replaceFirst("file:", "").replaceFirst("!.+", "");
                File MODS_DIR = new File(path).getParentFile();
                for(File file : Objects.requireNonNull(MODS_DIR.listFiles())){
                    String fileName = file.getName().toLowerCase();
                    if(fileName.endsWith(".jar") == true && fileName.contains("griefprevention")){
                        griefPreventionPresent = true;
                        break;
                    }
                }
                if(griefPreventionPresent == true){
                    LOGGER.info("GriefPrevention is present!");
                }else{
                    LOGGER.info("GriefPrevention is not present!");
                    LOGGER.info("If you do have GriefPrevention installed, please put the jar in the mods folder, and");
                    LOGGER.info("make sure that the the filename contains 'griefprevention' and ends with '.jar'");
                }
            }else{
                LOGGER.info("We're running in a deobfuscated environment.");
                try {
                    Class.forName("me.ryanhamshire.griefprevention.GriefPreventionPlugin", false, getClass().getClassLoader());
                    griefPreventionPresent = true;
                    LOGGER.info("GriefPrevention is present!");
                } catch (ClassNotFoundException ignored) {
                    LOGGER.info("GriefPrevention is not present!");
                }
            }


        }
    }

    private boolean shouldApplyMixin(String mixin){
        if(spongePresent == true){
            switch (mixin){
                case "cf.terminator.tiquality.mixin.MixinWorldServerForge":
                case "cf.terminator.tiquality.mixin.MixinWorldForge":
                    return false;
            }
        }else{
            switch (mixin){
                case "cf.terminator.tiquality.mixin.MixinTrackingUtilSpongeWorkaround":
                    return false;
            }
        }
        if(hasClientClasses == false){
            switch (mixin){
                case "cf.terminator.tiquality.mixin.MixinChunkProviderClient":
                case "cf.terminator.tiquality.mixin.MixinWorldClient":
                    return false;
            }
        }
        /*
         * Not really needed, but it makes our point extra clear.
         */
        if(griefPreventionPresent == false){
            switch (mixin){
                case "cf.terminator.tiquality.mixin.integration.griefprevention.MixinGPClaim":
                    return false;
            }
        }
        return true;
    }
    @Override
    public void onLoad(String mixinPackage) {
        setup();
    }

    @Override
    public String getRefMapperConfig() {
        return "mixins.tiquality.refmap.json";
    }


    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        boolean shouldApply = shouldApplyMixin(mixin);
        LOGGER.info((shouldApply ? "Enabling: " : "Skipping: ") + mixin);
        return shouldApply;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        List<String> list = new ArrayList<>();
        if(griefPreventionPresent){
            //TODO: Remove this, and ask for a better event instead of trying to use Mixin.
            //This works perfectly in development environments, but not in deobfuscated environments.
            //Mixin cannot find the target class, perhaps because it's nested?
            //list.add("integration.griefprevention.MixinGPClaim");
        }
        return list;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }


}
