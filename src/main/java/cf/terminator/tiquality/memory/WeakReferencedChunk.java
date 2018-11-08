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
     * @param other o
     * @return e
     */
    @Override
    public boolean equals(Object other){
        if(other instanceof WeakReferencedChunk == false){
            return false;
        }
        return get() == ((WeakReferencedChunk) other).get();
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
}
