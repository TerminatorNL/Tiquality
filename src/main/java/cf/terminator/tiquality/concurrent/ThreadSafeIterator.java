package cf.terminator.tiquality.concurrent;

import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Simply a wrapper for ReentrantLock around an iterator.
 * @param <E> e
 */
public class ThreadSafeIterator<E> implements Iterator<E> {

    private ReentrantLock LOCK;

    private Iterator<E> delegate;

    public ThreadSafeIterator(Iterator<E> delegate, ReentrantLock LOCK){
        this.LOCK = LOCK;
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        LOCK.lock();
        try{
            return delegate.hasNext();
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public E next() {
        LOCK.lock();
        try{
            return delegate.next();
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public void remove() {
        LOCK.lock();
        try{
            delegate.remove();
        }finally {
            LOCK.unlock();
        }
    }

    @Override
    public void forEachRemaining(Consumer<? super E> action) {
        LOCK.lock();
        try{
            delegate.forEachRemaining(action);
        }finally {
            LOCK.unlock();
        }
    }

}
