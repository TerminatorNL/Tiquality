package cf.terminator.tiquality.concurrent;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Simply a wrapper for ReentrantLock around a set.
 * @param <E> e
 */
public class ThreadSafeSet<E> implements Set<E> {

    private Set<E> data;
    private ReentrantLock LOCK = new ReentrantLock();

    public ThreadSafeSet(Set<E> data){
        this.data = data;
    }

    public void lock(){
        LOCK.lock();
    }

    public void unlock(){
        LOCK.unlock();
    }

    @Nonnull
    @Override
    public ThreadSafeIterator<E> iterator() {
        return new ThreadSafeIterator<>(data.iterator(), LOCK);
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        LOCK.lock();
        try{
            return data.toArray();
        }finally {
            LOCK.unlock();
        }
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        LOCK.lock();
        try{
            //noinspection SuspiciousToArrayCall
            return data.toArray(a);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public boolean add(E e) {
        LOCK.lock();
        try{
            return data.add(e);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        LOCK.lock();
        try{
            return data.remove(o);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        LOCK.lock();
        try{
            return data.containsAll(c);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        LOCK.lock();
        try{
            return data.addAll(c);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        LOCK.lock();
        try{
            return data.retainAll(c);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        LOCK.lock();
        try{
            return data.removeAll(c);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public void clear() {
        LOCK.lock();
        try{
            data.clear();
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public int size() {
        LOCK.lock();
        try{
            return data.size();
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        LOCK.lock();
        try{
            return data.isEmpty();
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        LOCK.lock();
        try{
            return data.contains(o);
        }finally {
            LOCK.unlock();
        }
    }


    @Override
    public boolean equals(Object o) {
        if(o instanceof Set == false){
            return false;
        }
        if(o == this){
            return true;
        }
        LOCK.lock();
        try{
            return data.equals(o);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public int hashCode() {
        LOCK.lock();
        try{
            return data.hashCode();
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        throw new UnsupportedOperationException("Spliterator is not allowed");
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        LOCK.lock();
        try{
            return data.removeIf(filter);
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public Stream<E> stream() {
        throw new UnsupportedOperationException("Stream is not allowed");
    }

    @Override
    public Stream<E> parallelStream() {
        throw new UnsupportedOperationException("Stream is not allowed");
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        LOCK.lock();
        try{
            data.forEach(action);
        }finally {
            LOCK.unlock();
        }
    }

}
