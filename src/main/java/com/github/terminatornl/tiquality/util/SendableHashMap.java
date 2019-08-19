package com.github.terminatornl.tiquality.util;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.HashMap;

/**
 * Provides a HashMap map that's able to be transmitted using Forge's IMessage
 */
public class SendableHashMap<K extends IMessage, V extends IMessage> extends HashMap<K, V> implements IMessage {

    public SendableHashMap() {
        super();
    }

    /**
     * Makes sure all class types are the same. Will throw an error if that's not the case.
     *
     * @return ClassTypeHolder with the detected class types.
     */
    private ClassTypeHolder checkClassTypes() {
        Class keyClass = null;
        Class valueClass = null;
        for (Entry<K, V> e : super.entrySet()) {
            if (keyClass == null) {
                keyClass = e.getKey().getClass();
                valueClass = e.getValue().getClass();
            } else if (keyClass != e.getKey().getClass()) {
                throw new IllegalStateException("Different instance types (classes) found in the keys of the map! This is not allowed!");
            } else if (valueClass != e.getValue().getClass()) {
                throw new IllegalStateException("Different instance types (classes) found in the values of the map! This is not allowed!");
            }
        }
        return new ClassTypeHolder(keyClass, valueClass);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ClassTypeHolder types = checkClassTypes();
        ByteBufUtils.writeUTF8String(buf, types.key.getName());
        ByteBufUtils.writeUTF8String(buf, types.value.getName());
        buf.writeInt(super.size());
        for (Entry<K, V> e : super.entrySet()) {
            e.getKey().toBytes(buf);
            e.getValue().toBytes(buf);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            Class<K> keyClass = (Class<K>) Class.forName(ByteBufUtils.readUTF8String(buf));
            Class<V> valueClass = (Class<V>) Class.forName(ByteBufUtils.readUTF8String(buf));
            int size = buf.readInt();
            for (int i = 0; i < size; ++i) {
                K key = keyClass.newInstance();
                key.fromBytes(buf);

                V value = valueClass.newInstance();
                value.fromBytes(buf);

                super.put(key, value);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ClassTypeHolder {
        final Class key;
        final Class value;

        private ClassTypeHolder(Class key, Class value) {
            this.key = key;
            this.value = value;
        }
    }

}
