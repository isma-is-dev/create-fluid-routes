package dev.ismadev.fluidroutes.data;

/**
 * Node categories for the fluid overlay.
 */
public final class NodeKind {

    public static final int PUMP = 0;
    public static final int TANK = 1;
    public static final int SPOUT = 2;
    /** A pipe that is not connected to anything, drawn as a marker so it stays visible. */
    public static final int PIPE = 3;

    private NodeKind() {
    }

    public static String translationKey(int kind) {
        return switch (kind) {
            case PUMP -> "fluidroutes.node.pump";
            case TANK -> "fluidroutes.node.tank";
            case SPOUT -> "fluidroutes.node.spout";
            default -> "fluidroutes.node.pipe";
        };
    }
}
