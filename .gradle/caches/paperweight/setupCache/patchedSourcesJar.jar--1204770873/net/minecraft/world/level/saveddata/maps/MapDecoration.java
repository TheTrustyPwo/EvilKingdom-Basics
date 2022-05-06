package net.minecraft.world.level.saveddata.maps;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class MapDecoration {
    private final MapDecoration.Type type;
    private final byte x;
    private final byte y;
    private final byte rot;
    @Nullable
    private final Component name;

    public MapDecoration(MapDecoration.Type type, byte x, byte z, byte rotation, @Nullable Component text) {
        this.type = type;
        this.x = x;
        this.y = z;
        this.rot = rotation;
        this.name = text;
    }

    public byte getImage() {
        return this.type.getIcon();
    }

    public MapDecoration.Type getType() {
        return this.type;
    }

    public byte getX() {
        return this.x;
    }

    public byte getY() {
        return this.y;
    }

    public byte getRot() {
        return this.rot;
    }

    public boolean renderOnFrame() {
        return this.type.isRenderedOnFrame();
    }

    @Nullable
    public Component getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof MapDecoration)) {
            return false;
        } else {
            MapDecoration mapDecoration = (MapDecoration)object;
            return this.type == mapDecoration.type && this.rot == mapDecoration.rot && this.x == mapDecoration.x && this.y == mapDecoration.y && Objects.equals(this.name, mapDecoration.name);
        }
    }

    @Override
    public int hashCode() {
        int i = this.type.getIcon();
        i = 31 * i + this.x;
        i = 31 * i + this.y;
        i = 31 * i + this.rot;
        return 31 * i + Objects.hashCode(this.name);
    }

    public static enum Type {
        PLAYER(false, true),
        FRAME(true, true),
        RED_MARKER(false, true),
        BLUE_MARKER(false, true),
        TARGET_X(true, false),
        TARGET_POINT(true, false),
        PLAYER_OFF_MAP(false, true),
        PLAYER_OFF_LIMITS(false, true),
        MANSION(true, 5393476, false),
        MONUMENT(true, 3830373, false),
        BANNER_WHITE(true, true),
        BANNER_ORANGE(true, true),
        BANNER_MAGENTA(true, true),
        BANNER_LIGHT_BLUE(true, true),
        BANNER_YELLOW(true, true),
        BANNER_LIME(true, true),
        BANNER_PINK(true, true),
        BANNER_GRAY(true, true),
        BANNER_LIGHT_GRAY(true, true),
        BANNER_CYAN(true, true),
        BANNER_PURPLE(true, true),
        BANNER_BLUE(true, true),
        BANNER_BROWN(true, true),
        BANNER_GREEN(true, true),
        BANNER_RED(true, true),
        BANNER_BLACK(true, true),
        RED_X(true, false);

        private final byte icon;
        private final boolean renderedOnFrame;
        private final int mapColor;
        private final boolean trackCount;

        private Type(boolean alwaysRender, boolean bl) {
            this(alwaysRender, -1, bl);
        }

        private Type(boolean alwaysRender, int tintColor, boolean bl) {
            this.trackCount = bl;
            this.icon = (byte)this.ordinal();
            this.renderedOnFrame = alwaysRender;
            this.mapColor = tintColor;
        }

        public byte getIcon() {
            return this.icon;
        }

        public boolean isRenderedOnFrame() {
            return this.renderedOnFrame;
        }

        public boolean hasMapColor() {
            return this.mapColor >= 0;
        }

        public int getMapColor() {
            return this.mapColor;
        }

        public static MapDecoration.Type byIcon(byte id) {
            return values()[Mth.clamp(id, 0, values().length - 1)];
        }

        public boolean shouldTrackCount() {
            return this.trackCount;
        }
    }
}
