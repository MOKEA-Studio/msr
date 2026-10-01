package kr.mokea.msr.estate;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EstateDataTest {
    @Test
    void purchaseAndPersistenceKeepBalancesAndOwnership() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        EstateData data = new EstateData();
        data.credit(alice, 205_000);

        assertTrue(data.buy(alice, "Alice", "minecraft:overworld:2:-1"));
        assertEquals(105_000, data.balance(alice));
        assertEquals(1, data.claimCount(alice));
        assertEquals(new EstateClaimsPayload.Chunk(2, -1),
                data.ownedChunks(alice, "minecraft:overworld").getFirst());
        assertFalse(data.buy(bob, "Bob", "minecraft:overworld:2:-1"));
        assertEquals(0, data.balance(bob));

        EstateData restored = EstateData.load(data.save(new CompoundTag(), null), null);
        assertEquals(105_000, restored.balance(alice));
        assertEquals(alice, restored.claim("minecraft:overworld:2:-1").owner());
        assertEquals(1, restored.ownedChunks(alice, "minecraft:overworld").size());
        assertFalse(restored.release(bob, "minecraft:overworld:2:-1"));
        assertTrue(restored.release(alice, "minecraft:overworld:2:-1"));
        assertEquals(105_000, restored.balance(alice)); // Releasing does not refund.
    }
}
