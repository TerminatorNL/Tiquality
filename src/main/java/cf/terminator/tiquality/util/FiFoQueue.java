package cf.terminator.tiquality.util;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;

public class FiFoQueue<T> extends LinkedList<T> {

    /**
     * First in First out array.
     */
    public FiFoQueue(){
        super();
    }

    public void addToQueue(T obj){
        super.addLast(obj);
    }

    public T take(){
        return super.removeFirst();
    }

    /**
     * Simular to the contains method, but does not rely on proper and fast
     * implementations for the .equals() method.
     *
     * This does an object to object comparison. Use with caution.
     *
     * @param ref the object
     * @return true if the object is present.
     */
    public boolean containsRef(@Nonnull T ref){
        Iterator<T> iterator = super.iterator();
        while(iterator.hasNext()){
            if(iterator.next() == ref){
                return true;
            }
        }
        return false;
    }


}
