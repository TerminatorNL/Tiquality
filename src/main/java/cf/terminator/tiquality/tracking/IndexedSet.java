package cf.terminator.tiquality.tracking;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Much like a HashMap, but for small sets
 * @param <V>
 */
public class IndexedSet<V extends IndexedSet.Element> {

    private ReentrantLock lock = new ReentrantLock();
    private HashMap<Integer, Entry> ENTRIES = new HashMap<>();

    public int size() {
        int count = 0;
        for(Entry e : ENTRIES.values()){
            count += e.elements.size();
        }
        return count;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(V o) {

        return false;
    }

    public boolean add(V e) {
        lock.lock();
        try {
            int index = e.getHash();

            Entry entry = ENTRIES.get(index);
            if (entry == null) {
                entry = new Entry(index);
            }
            return entry.elements.add(e);
        }finally {
            lock.unlock();
        }
    }

    public boolean remove(V o) {
        return false;
    }

    public void clear() {
        ENTRIES.clear();
    }

    interface Element{
        int getHash();
    }

    private class Entry implements Comparable{

        private final int index;
        private final TreeSet<V> elements;

        private Entry(int index){
            this.index = index;
            this.elements = new TreeSet<>();
        }

        private void add(V e){

        }


        @SuppressWarnings("unchecked")
        @Override
        public int compareTo(@Nonnull Object o) {
            return Integer.compare(index,((Entry) o).index);
        }

        @Override
        public int hashCode(){
            return index;
        }
    }
}
