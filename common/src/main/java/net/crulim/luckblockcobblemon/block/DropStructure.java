package net.crulim.luckblockcobblemon.block;

public class DropStructure {
    private String structureName;
    private int weight;

    public DropStructure(String structureName, int weight) {
        this.structureName = structureName;
        this.weight = weight;
    }

    public String getStructureName() {
        return structureName;
    }

    public int getWeight() {
        return weight;
    }
}
