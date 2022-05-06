package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public enum Mirror {
    NONE(new TranslatableComponent("mirror.none"), OctahedralGroup.IDENTITY),
    LEFT_RIGHT(new TranslatableComponent("mirror.left_right"), OctahedralGroup.INVERT_Z),
    FRONT_BACK(new TranslatableComponent("mirror.front_back"), OctahedralGroup.INVERT_X);

    private final Component symbol;
    private final OctahedralGroup rotation;

    private Mirror(Component name, OctahedralGroup directionTransformation) {
        this.symbol = name;
        this.rotation = directionTransformation;
    }

    public int mirror(int rotation, int fullTurn) {
        int i = fullTurn / 2;
        int j = rotation > i ? rotation - fullTurn : rotation;
        switch(this) {
        case FRONT_BACK:
            return (fullTurn - j) % fullTurn;
        case LEFT_RIGHT:
            return (i - j + fullTurn) % fullTurn;
        default:
            return rotation;
        }
    }

    public Rotation getRotation(Direction direction) {
        Direction.Axis axis = direction.getAxis();
        return (this != LEFT_RIGHT || axis != Direction.Axis.Z) && (this != FRONT_BACK || axis != Direction.Axis.X) ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    public Direction mirror(Direction direction) {
        if (this == FRONT_BACK && direction.getAxis() == Direction.Axis.X) {
            return direction.getOpposite();
        } else {
            return this == LEFT_RIGHT && direction.getAxis() == Direction.Axis.Z ? direction.getOpposite() : direction;
        }
    }

    public OctahedralGroup rotation() {
        return this.rotation;
    }

    public Component symbol() {
        return this.symbol;
    }
}