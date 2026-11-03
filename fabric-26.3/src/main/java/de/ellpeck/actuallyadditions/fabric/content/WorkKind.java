package de.ellpeck.actuallyadditions.fabric.content;
public enum WorkKind {
    PRESS("canola_press",40000,100,1),
    FERMENT("fermenting_barrel",0,0,0),
    OIL("oil_generator",50000,0,0),
    BREAKER("breaker",0,0,9),
    PLACER("placer",0,0,9),
    DROPPER("dropper",0,0,9),
    LONG_BREAKER("long_range_breaker",10000,20,9),
    HEAT("heat_collector",30000,0,0),
    LEAF("leaf_generator",35000,0,0),
    BIO("bio_reactor",200000,0,8),
    ENERGIZER("energizer",50000,1000,2),
    ENERVATOR("enervator",50000,1000,2),
    BATTERY_BOX("battery_box",0,100000,1),
    RECONSTRUCTOR("atomic_reconstructor",300000,5000,1),
    EMPOWERER("empowerer",0,0,1),
    DISPLAY("display_stand",80000,1000,1),
    FURNACE("powered_furnace",30000,80,6),
    CRUSHER("crusher",60000,100,3),
    DOUBLE_CRUSHER("crusher_double",60000,100,6);
    public final String id;
    public final int capacity, inputRate, slots;
    WorkKind(String id,int capacity,int inputRate,int slots){this.id=id;this.capacity=capacity;this.inputRate=inputRate;this.slots=slots;}
}
