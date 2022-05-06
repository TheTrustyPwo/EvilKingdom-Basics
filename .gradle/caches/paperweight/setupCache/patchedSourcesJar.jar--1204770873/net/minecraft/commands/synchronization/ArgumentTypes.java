package net.minecraft.commands.synchronization;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ItemEnchantmentArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.MobEffectArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceOrTagLocationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.synchronization.brigadier.BrigadierArgumentSerializers;
import net.minecraft.gametest.framework.TestClassNameArgument;
import net.minecraft.gametest.framework.TestFunctionArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class ArgumentTypes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Class<?>, ArgumentTypes.Entry<?>> BY_CLASS = Maps.newHashMap();
    private static final Map<ResourceLocation, ArgumentTypes.Entry<?>> BY_NAME = Maps.newHashMap();

    public static <T extends ArgumentType<?>> void register(String id, Class<T> argClass, ArgumentSerializer<T> serializer) {
        ResourceLocation resourceLocation = new ResourceLocation(id);
        if (BY_CLASS.containsKey(argClass)) {
            throw new IllegalArgumentException("Class " + argClass.getName() + " already has a serializer!");
        } else if (BY_NAME.containsKey(resourceLocation)) {
            throw new IllegalArgumentException("'" + resourceLocation + "' is already a registered serializer!");
        } else {
            ArgumentTypes.Entry<T> entry = new ArgumentTypes.Entry<>(serializer, resourceLocation);
            BY_CLASS.put(argClass, entry);
            BY_NAME.put(resourceLocation, entry);
        }
    }

    public static void bootStrap() {
        BrigadierArgumentSerializers.bootstrap();
        register("entity", EntityArgument.class, new EntityArgument.Serializer());
        register("game_profile", GameProfileArgument.class, new EmptyArgumentSerializer<>(GameProfileArgument::gameProfile));
        register("block_pos", BlockPosArgument.class, new EmptyArgumentSerializer<>(BlockPosArgument::blockPos));
        register("column_pos", ColumnPosArgument.class, new EmptyArgumentSerializer<>(ColumnPosArgument::columnPos));
        register("vec3", Vec3Argument.class, new EmptyArgumentSerializer<>(Vec3Argument::vec3));
        register("vec2", Vec2Argument.class, new EmptyArgumentSerializer<>(Vec2Argument::vec2));
        register("block_state", BlockStateArgument.class, new EmptyArgumentSerializer<>(BlockStateArgument::block));
        register("block_predicate", BlockPredicateArgument.class, new EmptyArgumentSerializer<>(BlockPredicateArgument::blockPredicate));
        register("item_stack", ItemArgument.class, new EmptyArgumentSerializer<>(ItemArgument::item));
        register("item_predicate", ItemPredicateArgument.class, new EmptyArgumentSerializer<>(ItemPredicateArgument::itemPredicate));
        register("color", ColorArgument.class, new EmptyArgumentSerializer<>(ColorArgument::color));
        register("component", ComponentArgument.class, new EmptyArgumentSerializer<>(ComponentArgument::textComponent));
        register("message", MessageArgument.class, new EmptyArgumentSerializer<>(MessageArgument::message));
        register("nbt_compound_tag", CompoundTagArgument.class, new EmptyArgumentSerializer<>(CompoundTagArgument::compoundTag));
        register("nbt_tag", NbtTagArgument.class, new EmptyArgumentSerializer<>(NbtTagArgument::nbtTag));
        register("nbt_path", NbtPathArgument.class, new EmptyArgumentSerializer<>(NbtPathArgument::nbtPath));
        register("objective", ObjectiveArgument.class, new EmptyArgumentSerializer<>(ObjectiveArgument::objective));
        register("objective_criteria", ObjectiveCriteriaArgument.class, new EmptyArgumentSerializer<>(ObjectiveCriteriaArgument::criteria));
        register("operation", OperationArgument.class, new EmptyArgumentSerializer<>(OperationArgument::operation));
        register("particle", ParticleArgument.class, new EmptyArgumentSerializer<>(ParticleArgument::particle));
        register("angle", AngleArgument.class, new EmptyArgumentSerializer<>(AngleArgument::angle));
        register("rotation", RotationArgument.class, new EmptyArgumentSerializer<>(RotationArgument::rotation));
        register("scoreboard_slot", ScoreboardSlotArgument.class, new EmptyArgumentSerializer<>(ScoreboardSlotArgument::displaySlot));
        register("score_holder", ScoreHolderArgument.class, new ScoreHolderArgument.Serializer());
        register("swizzle", SwizzleArgument.class, new EmptyArgumentSerializer<>(SwizzleArgument::swizzle));
        register("team", TeamArgument.class, new EmptyArgumentSerializer<>(TeamArgument::team));
        register("item_slot", SlotArgument.class, new EmptyArgumentSerializer<>(SlotArgument::slot));
        register("resource_location", ResourceLocationArgument.class, new EmptyArgumentSerializer<>(ResourceLocationArgument::id));
        register("mob_effect", MobEffectArgument.class, new EmptyArgumentSerializer<>(MobEffectArgument::effect));
        register("function", FunctionArgument.class, new EmptyArgumentSerializer<>(FunctionArgument::functions));
        register("entity_anchor", EntityAnchorArgument.class, new EmptyArgumentSerializer<>(EntityAnchorArgument::anchor));
        register("int_range", RangeArgument.Ints.class, new EmptyArgumentSerializer<>(RangeArgument::intRange));
        register("float_range", RangeArgument.Floats.class, new EmptyArgumentSerializer<>(RangeArgument::floatRange));
        register("item_enchantment", ItemEnchantmentArgument.class, new EmptyArgumentSerializer<>(ItemEnchantmentArgument::enchantment));
        register("entity_summon", EntitySummonArgument.class, new EmptyArgumentSerializer<>(EntitySummonArgument::id));
        register("dimension", DimensionArgument.class, new EmptyArgumentSerializer<>(DimensionArgument::dimension));
        register("time", TimeArgument.class, new EmptyArgumentSerializer<>(TimeArgument::time));
        register("uuid", UuidArgument.class, new EmptyArgumentSerializer<>(UuidArgument::uuid));
        register("resource", fixClassType(ResourceKeyArgument.class), new ResourceKeyArgument.Serializer());
        register("resource_or_tag", fixClassType(ResourceOrTagLocationArgument.class), new ResourceOrTagLocationArgument.Serializer());
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            register("test_argument", TestFunctionArgument.class, new EmptyArgumentSerializer<>(TestFunctionArgument::testFunctionArgument));
            register("test_class", TestClassNameArgument.class, new EmptyArgumentSerializer<>(TestClassNameArgument::testClassName));
        }

    }

    private static <T extends ArgumentType<?>> Class<T> fixClassType(Class<? super T> clazz) {
        return clazz;
    }

    @Nullable
    private static ArgumentTypes.Entry<?> get(ResourceLocation id) {
        return BY_NAME.get(id);
    }

    @Nullable
    private static ArgumentTypes.Entry<?> get(ArgumentType<?> type) {
        return BY_CLASS.get(type.getClass());
    }

    public static <T extends ArgumentType<?>> void serialize(FriendlyByteBuf buf, T type) {
        ArgumentTypes.Entry<T> entry = get(type);
        if (entry == null) {
            LOGGER.error("Could not serialize {} ({}) - will not be sent to client!", type, type.getClass());
            buf.writeResourceLocation(new ResourceLocation(""));
        } else {
            buf.writeResourceLocation(entry.name);
            entry.serializer.serializeToNetwork(type, buf);
        }
    }

    @Nullable
    public static ArgumentType<?> deserialize(FriendlyByteBuf buf) {
        ResourceLocation resourceLocation = buf.readResourceLocation();
        ArgumentTypes.Entry<?> entry = get(resourceLocation);
        if (entry == null) {
            LOGGER.error("Could not deserialize {}", (Object)resourceLocation);
            return null;
        } else {
            return entry.serializer.deserializeFromNetwork(buf);
        }
    }

    private static <T extends ArgumentType<?>> void serializeToJson(JsonObject json, T type) {
        ArgumentTypes.Entry<T> entry = get(type);
        if (entry == null) {
            LOGGER.error("Could not serialize argument {} ({})!", type, type.getClass());
            json.addProperty("type", "unknown");
        } else {
            json.addProperty("type", "argument");
            json.addProperty("parser", entry.name.toString());
            JsonObject jsonObject = new JsonObject();
            entry.serializer.serializeToJson(type, jsonObject);
            if (jsonObject.size() > 0) {
                json.add("properties", jsonObject);
            }
        }

    }

    public static <S> JsonObject serializeNodeToJson(CommandDispatcher<S> dispatcher, CommandNode<S> commandNode) {
        JsonObject jsonObject = new JsonObject();
        if (commandNode instanceof RootCommandNode) {
            jsonObject.addProperty("type", "root");
        } else if (commandNode instanceof LiteralCommandNode) {
            jsonObject.addProperty("type", "literal");
        } else if (commandNode instanceof ArgumentCommandNode) {
            serializeToJson(jsonObject, ((ArgumentCommandNode)commandNode).getType());
        } else {
            LOGGER.error("Could not serialize node {} ({})!", commandNode, commandNode.getClass());
            jsonObject.addProperty("type", "unknown");
        }

        JsonObject jsonObject2 = new JsonObject();

        for(CommandNode<S> commandNode2 : commandNode.getChildren()) {
            jsonObject2.add(commandNode2.getName(), serializeNodeToJson(dispatcher, commandNode2));
        }

        if (jsonObject2.size() > 0) {
            jsonObject.add("children", jsonObject2);
        }

        if (commandNode.getCommand() != null) {
            jsonObject.addProperty("executable", true);
        }

        if (commandNode.getRedirect() != null) {
            Collection<String> collection = dispatcher.getPath(commandNode.getRedirect());
            if (!collection.isEmpty()) {
                JsonArray jsonArray = new JsonArray();

                for(String string : collection) {
                    jsonArray.add(string);
                }

                jsonObject.add("redirect", jsonArray);
            }
        }

        return jsonObject;
    }

    public static boolean isTypeRegistered(ArgumentType<?> type) {
        return get(type) != null;
    }

    public static <T> Set<ArgumentType<?>> findUsedArgumentTypes(CommandNode<T> node) {
        Set<CommandNode<T>> set = Sets.newIdentityHashSet();
        Set<ArgumentType<?>> set2 = Sets.newHashSet();
        findUsedArgumentTypes(node, set2, set);
        return set2;
    }

    private static <T> void findUsedArgumentTypes(CommandNode<T> node, Set<ArgumentType<?>> argumentTypes, Set<CommandNode<T>> ignoredNodes) {
        if (ignoredNodes.add(node)) {
            if (node instanceof ArgumentCommandNode) {
                argumentTypes.add(((ArgumentCommandNode)node).getType());
            }

            node.getChildren().forEach((nodex) -> {
                findUsedArgumentTypes(nodex, argumentTypes, ignoredNodes);
            });
            CommandNode<T> commandNode = node.getRedirect();
            if (commandNode != null) {
                findUsedArgumentTypes(commandNode, argumentTypes, ignoredNodes);
            }

        }
    }

    static class Entry<T extends ArgumentType<?>> {
        public final ArgumentSerializer<T> serializer;
        public final ResourceLocation name;

        Entry(ArgumentSerializer<T> serializer, ResourceLocation id) {
            this.serializer = serializer;
            this.name = id;
        }
    }
}