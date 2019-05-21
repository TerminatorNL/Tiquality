package cf.terminator.tiquality.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Keep in mind the data in this class is treated as bytes, even during bitwise operations, I chose
 * bytes over Integers because they take less RAM.
 * It is known that these bytes are promoted to integers during the bitwise operations.
 */
public class ChunkStorage {

    private Element[] data = new Element[16];

    public ChunkStorage(){
    }

    /**
     * Removes all data from this ChunkStorage.
     */
    public void clearAll(){
        data = new Element[16];
    }

    /**
     * Sets all data from this ChunkStorage.
     */
    public void setAll(byte owner_id){
        data = new Element[16];

        byte[] storage = new byte[4096];
        Arrays.fill(storage, owner_id);

        for(int i=0;i<data.length;i++){
            data[i] = new Element(storage);
        }
    }

    /**
     * Gets the stored owner associated with the BlockPos.
     * @param pos the block pos
     * @return the owner ID, or 1 if none is found
     */
    public byte get(BlockPos pos){
        int y_layer =  pos.getY() >> 4;
        return data[y_layer] == null ? 1 : data[y_layer].get(pos);
    }

    /**
     * Sets the stored owner associated with the BlockPos.
     * @param pos the block pos
     * @param owner_id the owner ID
     */
    public void set(BlockPos pos, byte owner_id){
        int y_layer =  pos.getY() >> 4;
        if(data[y_layer] == null) {
            data[y_layer] = new Element();
        }
        data[y_layer].set(pos, owner_id);
    }

    /**
     * Marks a block position
     * @param pos the pos
     */
    public void mark(BlockPos pos){
        int y_layer =  pos.getY() >> 4;
        if(data[y_layer] == null) {
            data[y_layer] = new Element();
        }
        data[y_layer].mark(pos);
    }

    /**
     * Unmarks a block position
     * @param pos the pos
     */
    public void unMark(BlockPos pos){
        int y_layer =  pos.getY() >> 4;
        if(data[y_layer] == null) {
            data[y_layer] = new Element();
        }
        data[y_layer].unmark(pos);
    }

    /**
     * Checks if a position is marked.
     * @param pos the position
     * @return marked
     */
    public boolean isMarked(BlockPos pos){
        int y_layer =  pos.getY() >> 4;
        return data[y_layer] != null && data[y_layer].isMarked(pos);
    }


    public ArrayList<byte[]> getAll(){
        ArrayList<byte[]> list = new ArrayList<>();
        for (Element e : data) {
            if (e != null) {
                if (e.hasData()) {
                    list.add(e.getUnmarkedCopy());
                }
            }
        }
        return list;
    }

    /**
     * Replaces a tracker with another tracker
     * @param old the old tracker
     * @param new_ the new tracker
     */
    public void replaceAll(byte old, byte new_){
        for (Element e : data) {
            if (e != null) {
                e.replaceAll(old, new_);
            }
        }
    }

    public void loadFromNBT(@Nonnull NBTTagCompound list, @Nonnull Chunk chunk){
        for(String key : list.getKeySet()){
            int y = Integer.valueOf(key);
            byte[] storage = list.getByteArray(key);
            data[y] = new Element(storage);
            data[y].queueMarkedForUpdate(chunk, y);
        }
    }

    @Nullable
    public NBTTagCompound getNBT(){
        NBTTagCompound list = new NBTTagCompound();
        for(int i=0;i<data.length;++i){
            Element e = data[i];
            if(e != null && e.hasData()){
                list.setByteArray(Integer.toString(i),e.getData());
            }
        }
        return list.getSize() == 0 ? null : list;
    }

    public static class Element{

        private final byte[] storage;

        Element(){
            storage = new byte[4096];
        }

        /**
         * Creates a new storage element with the given array.
         * THIS MUST HAVE A LENGTH OF 4096.
         * @param storage byte array
         */
        Element(byte[] storage){
            this.storage = storage;
        }

        boolean hasData(){
            for(byte b : storage){
                if(b != 0 && b != 1){
                    return true;
                }
            }
            return false;
        }

        int getIndex(BlockPos pos){
            return (pos.getY() & 15) << 8 | (pos.getZ() & 15) << 4 | (pos.getX() & 15);
        }

        int getIndex(int x, int y, int z){
            return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        }

        /**
         * Gets the stored owner associated with the BlockPos.
         * @param pos the block pos
         * @return the owner ID, or 1 if none is found
         */
        byte get(BlockPos pos){
            return (byte) (storage[getIndex(pos)] & 127);
        }

        /**
         * Sets the stored owner associated with the BlockPos.
         * @param pos the block pos
         */
        void set(BlockPos pos, byte owner_id){
            storage[getIndex(pos)] = owner_id;
        }


        void replaceAll(byte old, byte new_) {
            for (int i = 0; i < storage.length; i++){
                if(storage[i] == old || (storage[i] ^ -128) == old){
                    storage[i] = new_;
                }
            }
        }

        byte[] getUnmarkedCopy(){
            byte[] copy = new byte[storage.length];
            for(int i_=0; i_<storage.length; i_++){
                copy[i_] = (byte) (storage[i_] & 127);
            }
            return copy;
        }

        byte[] getData(){
            return storage;
        }

        /**
         * Marks a block position
         * @param pos the block pos
         */
        void mark(BlockPos pos){
            storage[getIndex(pos)] |= -128;
        }

        /**
         * Unmarks a block position
         * @param pos the block pos
         */
        void unmark(BlockPos pos){
            storage[getIndex(pos)] &= 127;
        }

        /**
         * Checks if a block position is marked.
         * @param pos the block pos
         */
        boolean isMarked(BlockPos pos){
            return storage[getIndex(pos)] < 0;
        }

        /**
         * Tick all marked blocks for update.
         */
        public void queueMarkedForUpdate(Chunk chunk, int y_level) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            ChunkPos chunkPos = chunk.getPos();
            for(int x=chunkPos.getXStart(); x < chunkPos.getXEnd() ; x++){
                for(int y = y_level*16; y < y_level*16 + 16 ; y++){
                    for(int z=chunkPos.getZStart(); z < chunkPos.getZEnd() ; z++){
                        pos.setPos(x,y,z);
                        if(isMarked(pos)){
                            BlockPos realPos = pos.toImmutable();
                            IBlockState state = chunk.getBlockState(realPos);
                            unmark(realPos);
                            chunk.getWorld().scheduleBlockUpdate(realPos, state.getBlock(), 1, 0);
                        }
                    }
                }
            }
        }
    }
}
