package cf.terminator.tiquality.mixinhelper;

import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinConfigPlugin implements IMixinConfigPlugin {

    private boolean spongePresent = false;
    private boolean hasClientClasses = true;

    @Override
    public void onLoad(String s) {
        Logger LOGGER = LogManager.getLogger("Tiquality");
        if(getClass().getClassLoader().getResource("net/minecraft/client/main/Main.class") == null){
            hasClientClasses = false;
            LOGGER.info("Loading server classes");
        }else{
            LOGGER.info("Loading client classes");
        }
        try {
            Class.forName("org.spongepowered.mod.SpongeCoremod", false, getClass().getClassLoader());
            LOGGER.info("Sponge is present!");
            spongePresent = true;
        } catch (ClassNotFoundException ignored_1) {
            LOGGER.info("Sponge is not present!");
            spongePresent = false;
            try{
                Class.forName("org.spongepowered.asm.launch.MixinTweaker", false, getClass().getClassLoader());
            } catch (ClassNotFoundException ignored_2) {
                LOGGER.info("Oh no! It looks like you also do not have Mixin installed. Please use the FORGE version of ForgeCommand.");
                FMLCommonHandler.instance().exitJava(1, true);
            }
        }
    }

    @Override
    public String getRefMapperConfig() {
        return "mixins.tiquality.refmap.json";
    }

    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
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
        return true;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
