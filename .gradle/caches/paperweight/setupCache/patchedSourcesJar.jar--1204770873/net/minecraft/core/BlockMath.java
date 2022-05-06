package net.minecraft.core;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.Util;
import org.slf4j.Logger;

public class BlockMath {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<Direction, Transformation> VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL = Util.make(Maps.newEnumMap(Direction.class), (enumMap) -> {
        enumMap.put(Direction.SOUTH, Transformation.identity());
        enumMap.put(Direction.EAST, new Transformation((Vector3f)null, Vector3f.YP.rotationDegrees(90.0F), (Vector3f)null, (Quaternion)null));
        enumMap.put(Direction.WEST, new Transformation((Vector3f)null, Vector3f.YP.rotationDegrees(-90.0F), (Vector3f)null, (Quaternion)null));
        enumMap.put(Direction.NORTH, new Transformation((Vector3f)null, Vector3f.YP.rotationDegrees(180.0F), (Vector3f)null, (Quaternion)null));
        enumMap.put(Direction.UP, new Transformation((Vector3f)null, Vector3f.XP.rotationDegrees(-90.0F), (Vector3f)null, (Quaternion)null));
        enumMap.put(Direction.DOWN, new Transformation((Vector3f)null, Vector3f.XP.rotationDegrees(90.0F), (Vector3f)null, (Quaternion)null));
    });
    public static final Map<Direction, Transformation> VANILLA_UV_TRANSFORM_GLOBAL_TO_LOCAL = Util.make(Maps.newEnumMap(Direction.class), (enumMap) -> {
        for(Direction direction : Direction.values()) {
            enumMap.put(direction, VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL.get(direction).inverse());
        }

    });

    public static Transformation blockCenterToCorner(Transformation transformation) {
        Matrix4f matrix4f = Matrix4f.createTranslateMatrix(0.5F, 0.5F, 0.5F);
        matrix4f.multiply(transformation.getMatrix());
        matrix4f.multiply(Matrix4f.createTranslateMatrix(-0.5F, -0.5F, -0.5F));
        return new Transformation(matrix4f);
    }

    public static Transformation blockCornerToCenter(Transformation transformation) {
        Matrix4f matrix4f = Matrix4f.createTranslateMatrix(-0.5F, -0.5F, -0.5F);
        matrix4f.multiply(transformation.getMatrix());
        matrix4f.multiply(Matrix4f.createTranslateMatrix(0.5F, 0.5F, 0.5F));
        return new Transformation(matrix4f);
    }

    public static Transformation getUVLockTransform(Transformation transformation, Direction direction, Supplier<String> supplier) {
        Direction direction2 = Direction.rotate(transformation.getMatrix(), direction);
        Transformation transformation2 = transformation.inverse();
        if (transformation2 == null) {
            LOGGER.warn(supplier.get());
            return new Transformation((Vector3f)null, (Quaternion)null, new Vector3f(0.0F, 0.0F, 0.0F), (Quaternion)null);
        } else {
            Transformation transformation3 = VANILLA_UV_TRANSFORM_GLOBAL_TO_LOCAL.get(direction).compose(transformation2).compose(VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL.get(direction2));
            return blockCenterToCorner(transformation3);
        }
    }
}