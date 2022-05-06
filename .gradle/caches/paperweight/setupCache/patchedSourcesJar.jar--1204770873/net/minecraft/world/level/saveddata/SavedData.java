package net.minecraft.world.level.saveddata;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;

public abstract class SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean dirty;

    public abstract CompoundTag save(CompoundTag nbt);

    public void setDirty() {
        this.setDirty(true);
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void save(File file) {
        if (this.isDirty()) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put("data", this.save(new CompoundTag()));
            compoundTag.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());

            try {
                NbtIo.writeCompressed(compoundTag, file);
            } catch (IOException var4) {
                LOGGER.error("Could not save data {}", this, var4);
            }

            this.setDirty(false);
        }
    }
}
