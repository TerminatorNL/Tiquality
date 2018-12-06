package cf.terminator.tiquality.memory;

import cf.terminator.tiquality.interfaces.TiqualityChunk;

import java.lang.ref.WeakReference;

public class WeakReferencedChunk extends WeakReference<TiqualityChunk> {

    public WeakReferencedChunk(TiqualityChunk chunk) {
        super(chunk);
    }

    public boolean isChunkLoaded(){
        TiqualityChunk chunk = get();
        return chunk != null && chunk.isChunkLoaded();
    }

    /**
     * Used for HashSet
     * @return hashCode
     */
    @Override
    public int hashCode(){
        TiqualityChunk chunk = get();
        return chunk == null ? 0 : chunk.hashCode();
    }

    /**
     * Used for HashSet
     */
    @Override
    public boolean equals(Object o){
        if(o instanceof WeakReferencedChunk == false){
            return false;
        }
        WeakReferencedChunk other = (WeakReferencedChunk) o;
        TiqualityChunk thisChunk = get();
        TiqualityChunk otherChunk = other.get();

        if(this == o || thisChunk == otherChunk){
            return true;
        }
        if(thisChunk == null || otherChunk == null){
            return false;
        }

        return thisChunk.getMinecraftChunk().getPos().equals(otherChunk.getMinecraftChunk().getPos());
    }
}
