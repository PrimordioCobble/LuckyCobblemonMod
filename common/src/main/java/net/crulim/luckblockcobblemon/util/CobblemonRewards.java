package net.crulim.luckblockcobblemon.util;

import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.api.tms.TechnicalMachines;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * Helpers for Cobblemon 1.8 rewards: Type Gems, Type Gem Blocks and Technical Machines (TMs).
 */
public final class CobblemonRewards {
    private static final RandomSource RANDOM = RandomSource.create();

    /**
     * Elemental types covered by each themed Lucky Block (same pairing used by the themed item pools).
     */
    private static final Map<String, List<String>> THEMED_TYPES = new LinkedHashMap<>();

    static {
        THEMED_TYPES.put("fire", List.of("fire"));
        THEMED_TYPES.put("water", List.of("water", "ice"));
        THEMED_TYPES.put("grass", List.of("grass", "bug"));
        THEMED_TYPES.put("ground", List.of("ground", "fighting"));
        THEMED_TYPES.put("fly", List.of("flying", "dragon"));
        THEMED_TYPES.put("steel", List.of("steel", "rock"));
        THEMED_TYPES.put("eletric", List.of("electric", "dark"));
        THEMED_TYPES.put("fairy", List.of("fairy", "ghost", "poison"));
    }

    private CobblemonRewards() {
    }

    public static List<String> themedTypes(String themedPool) {
        if (themedPool == null) {
            return List.of();
        }
        return THEMED_TYPES.getOrDefault(themedPool.toLowerCase(Locale.ROOT), List.of());
    }

    public static List<String> typesOf(Pokemon pokemon) {
        List<String> types = new ArrayList<>();
        if (pokemon == null) {
            return types;
        }
        for (ElementalType type : pokemon.getTypes()) {
            if (type != null && type.getName() != null) {
                types.add(type.getName().toLowerCase(Locale.ROOT));
            }
        }
        return types;
    }

    public static List<String> readStringList(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        if (json == null || !json.has(key) || !json.get(key).isJsonArray()) {
            return values;
        }
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element != null && element.isJsonPrimitive()) {
                String value = element.getAsString().trim();
                if (!value.isEmpty()) {
                    values.add(value.toLowerCase(Locale.ROOT));
                }
            }
        }
        return values;
    }

    public static Item item(String id) {
        if (id == null || id.isBlank()) {
            return Items.AIR;
        }
        ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier == null) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(identifier);
    }

    public static ItemStack gemStack(String type, int count) {
        Item gem = item("cobblemon:" + type + "_gem");
        if (gem == Items.AIR || count <= 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(gem, count);
    }

    public static ItemStack gemBlockStack(String type) {
        Item block = item("cobblemon:" + type + "_gem_block");
        if (block == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(block, 1);
    }

    /**
     * Creates a random Technical Machine. If types is empty, any TM can be chosen.
     */
    public static ItemStack randomTm(Collection<String> types) {
        List<TechnicalMachine> candidates = new ArrayList<>();
        try {
            for (TechnicalMachine tm : TechnicalMachines.INSTANCE.getTmMap().values()) {
                if (tm == null) {
                    continue;
                }
                if (types == null || types.isEmpty()) {
                    candidates.add(tm);
                    continue;
                }
                String tmType = tm.getType() == null ? "" : tm.getType().toLowerCase(Locale.ROOT);
                if (types.contains(tmType)) {
                    candidates.add(tm);
                }
            }

            if (candidates.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = candidates.get(RANDOM.nextInt(candidates.size())).createItemStack();
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (Exception e) {
            System.out.println("[LuckyBlockCobblemon] Failed to create TM: " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    /**
     * Pool event of type "tm": drops between min and max random TMs filtered by tmTypes.
     * When tmTypes is missing or empty, fallbackTypes is used (empty = any type).
     */
    public static void dropRandomTms(ServerLevel world, BlockPos pos, JsonObject data, List<String> fallbackTypes) {
        List<String> types = readStringList(data, "tmTypes");
        if (types.isEmpty() && fallbackTypes != null) {
            types = fallbackTypes;
        }

        int min = data != null && data.has("min") ? data.get("min").getAsInt() : 1;
        int max = data != null && data.has("max") ? data.get("max").getAsInt() : min;
        int amount = randomBetween(min, max);

        for (int i = 0; i < amount; i++) {
            ItemStack tm = randomTm(types);
            if (!tm.isEmpty()) {
                Block.popResource(world, pos.above(), tm);
            }
        }
    }

    /**
     * Gems of the given types, split randomly between them, merged into stacks.
     */
    public static List<ItemStack> gemStacks(List<String> types, int amount) {
        List<ItemStack> stacks = new ArrayList<>();
        if (types == null || types.isEmpty() || amount <= 0) {
            return stacks;
        }

        Map<String, Integer> perType = new LinkedHashMap<>();
        for (int i = 0; i < amount; i++) {
            String type = types.get(RANDOM.nextInt(types.size()));
            perType.merge(type, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : perType.entrySet()) {
            ItemStack stack = gemStack(entry.getKey(), entry.getValue());
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    public static ItemStack randomItemFrom(JsonArray ids, int count) {
        if (ids == null || ids.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String id = ids.get(RANDOM.nextInt(ids.size())).getAsString();
        Item item = item(id);
        if (item == Items.AIR) {
            System.out.println("[LuckyBlockCobblemon] Invalid reward item id: " + id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, count));
    }

    public static boolean roll(float chancePercent) {
        return chancePercent > 0F && RANDOM.nextFloat() * 100F < chancePercent;
    }

    public static int randomBetween(int min, int max) {
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }
        return min + RANDOM.nextInt(max - min + 1);
    }
}
