package net.minecraft.server.packs.metadata.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;

public class PackMetadataSectionSerializer implements MetadataSectionSerializer<PackMetadataSection> {
    @Override
    public PackMetadataSection fromJson(JsonObject jsonObject) {
        Component component = Component.Serializer.fromJson(jsonObject.get("description"));
        if (component == null) {
            throw new JsonParseException("Invalid/missing description!");
        } else {
            int i = GsonHelper.getAsInt(jsonObject, "pack_format");
            return new PackMetadataSection(component, i);
        }
    }

    @Override
    public String getMetadataSectionName() {
        return "pack";
    }
}