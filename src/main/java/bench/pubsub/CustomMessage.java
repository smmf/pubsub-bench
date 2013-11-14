package bench.pubsub;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

public class CustomMessage implements DataSerializable, Externalizable {

    private static final long serialVersionUID = 1L;

    public static UUID UUID_RESERVED_SENTINEL = new UUID(0, 0);

    public static synchronized CustomMessage makeSentinel() {
        return new CustomMessage() {
            private static final long serialVersionUID = 2L;

            {
                this.id = UUID_RESERVED_SENTINEL;
            }

            @Override
            public String toString() {
                return "SENTINEL";
            }
        };
    }

    UUID id;
    byte[] data;
    private final AtomicReference<CustomMessage> next = new AtomicReference<>();

    public CustomMessage() {
    }

    public CustomMessage(int payload) {
        this.id = UUID.randomUUID();
        this.data = new byte[payload];
        ThreadLocalRandom.current().nextBytes(this.data);
    }

    public UUID getId() {
        return this.id;
    }

    public boolean setNext(CustomMessage next) {
        return this.next.compareAndSet(null, next);
    }

    public CustomMessage getNext() {
        return this.next.get();
    }

    // serialization code

    public int estimateSizeInBytes() {
        return 16 + 4 + this.data.length;
    }

    public void writeToDataOutput(DataOutput out) throws IOException {
        writeUUID(out);

        out.writeInt(this.data.length);
        out.write(this.data);
    }

    public void readFromDataInput(DataInput in) throws IOException {
        this.id = readUUID(in);

        this.data = new byte[in.readInt()];
        in.readFully(this.data);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        writeToDataOutput(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        readFromDataInput(in);
    }

    // Hazelcast's DataSerializable interface
    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        writeToDataOutput(out);
    }

    // Hazelcast's DataSerializable interface
    @Override
    public void readData(ObjectDataInput in) throws IOException {
        readFromDataInput(in);
    }

    private void writeUUID(DataOutput out) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
    }

    protected UUID readUUID(DataInput in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

}
