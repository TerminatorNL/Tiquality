package cf.terminator.tiquality.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static DecimalFormat TWO_DECIMAL_FORMATTER = new DecimalFormat("#0.00");

    public static class BlockPos{
        public static net.minecraft.util.math.BlockPos getMax(net.minecraft.util.math.BlockPos a, net.minecraft.util.math.BlockPos b){
            int high_x = Math.max(a.getX(), b.getX());
            int high_y = Math.max(a.getY(), b.getY());
            int high_z = Math.max(a.getZ(), b.getZ());
            return new net.minecraft.util.math.BlockPos(high_x, high_y, high_z);
        }

        public static net.minecraft.util.math.BlockPos getMin(net.minecraft.util.math.BlockPos a, net.minecraft.util.math.BlockPos b){
            int low_x = Math.min(a.getX(), b.getX());
            int low_y = Math.min(a.getY(), b.getY());
            int low_z = Math.min(a.getZ(), b.getZ());
            return new net.minecraft.util.math.BlockPos(low_x, low_y, low_z);
        }

        public static boolean isWithinBounds(net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.BlockPos a, net.minecraft.util.math.BlockPos b){
            net.minecraft.util.math.BlockPos lower = BlockPos.getMin(a, b);
            net.minecraft.util.math.BlockPos upper = BlockPos.getMax(a, b);

            boolean isLowerOK = pos.getX() >= lower.getX() && pos.getY() >= lower.getY() && pos.getZ() >= lower.getZ();
            if(isLowerOK == false){
                return false;
            }
            return pos.getX() <= upper.getX() && pos.getY() <= upper.getY() && pos.getZ() <= upper.getZ();
        }
    }

    public static class Chunk{

        public static boolean isWithinBounds(ChunkPos chunkPos, net.minecraft.util.math.BlockPos a, net.minecraft.util.math.BlockPos b){
            net.minecraft.util.math.BlockPos lower = BlockPos.getMin(a, b);
            net.minecraft.util.math.BlockPos upper = BlockPos.getMax(a, b);

            boolean startOK = BlockPos.isWithinBounds(new net.minecraft.util.math.BlockPos(chunkPos.getXStart(), lower.getY(), chunkPos.getZStart()), lower, upper);
            if(startOK == false){
                return false;
            }
            return BlockPos.isWithinBounds(new net.minecraft.util.math.BlockPos(chunkPos.getXEnd(), upper.getY(), chunkPos.getZEnd()), lower, upper);
        }

    }

    public static Vec3d getLookVec(Entity entity, int distance){
        return entity.getPositionEyes(1F).add(entity.getLookVec().scale(distance));
    }

    private static final HashMap<EntityPlayer, Map.Entry<Long,ITextComponent>> LAST_MESSAGES = new HashMap<>();
    public static void sendStatusBarMessage(EntityPlayer player, ITextComponent text){
        if(text == null || player == null){
            return;
        }
        if(LAST_MESSAGES.containsKey(player) == false || LAST_MESSAGES.get(player).getValue().getFormattedText().equals(text.getFormattedText()) == false || System.currentTimeMillis() > LAST_MESSAGES.get(player).getKey()){
            LAST_MESSAGES.put(player, new AbstractMap.SimpleEntry<>(System.currentTimeMillis() + 1500, text));
            player.sendStatusMessage(text, true);
        }
    }
}
