package net.crulim.luckblockcobblemon.block.custom;

import java.util.List;
import net.minecraft.util.math.random.Random;

public class LuckyBlockRandomizer<T> {

    private final List<T> options;
    private final Random random;
    private T lastResult;

    public LuckyBlockRandomizer(List<T> options) {
        this.options = options;
        this.random = Random.create();
        this.lastResult = null;
    }

    public T getRandom() {
        if (options.isEmpty()) {
            throw new IllegalStateException("Option list is empty!");
        }

        T selected = options.get(random.nextInt(options.size()));

        // Evita repetição imediata
        if (selected.equals(lastResult) && options.size() > 1) {
            selected = options.get(random.nextInt(options.size()));
        }

        lastResult = selected;
        return selected;
    }
}