package org.divinitycraft.divinityeconomy.console;

import org.divinitycraft.divinityeconomy.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Log levels for console messages with support for traditional ChatColor and HEX colors.
 * HEX colors are supported on Minecraft 1.16+ servers.
 */
public enum LogLevel {
    DEBUG(ChatColor.DARK_PURPLE, Setting.CHAT_DEBUG_COLOR.path),
    INFO(ChatColor.GREEN, Setting.CHAT_INFO_COLOR.path),
    WARNING(ChatColor.YELLOW, Setting.CHAT_WARNING_COLOR.path),
    SEVERE(ChatColor.DARK_RED, Setting.CHAT_SEVERE_COLOR.path),
    MIGRATE(ChatColor.GOLD, null);

    // Pattern for matching HEX color codes
    private static final Pattern HEX_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6})$");
    private static final Pattern HEX_PATTERN_WITH_AMP = Pattern.compile("^&#([A-Fa-f0-9]{6})$");

    private Object colour;  // Can be ChatColor or String (for hex colors)
    private final String colourOption;

    LogLevel(ChatColor color, String colourOption) {
        this.colour = color;
        this.colourOption = colourOption;
    }

    /**
     * Loads log level colors from the configuration file.
     * Supports both traditional ChatColor names and HEX color codes.
     * HEX format: #RRGGBB or &#RRGGBB (requires Minecraft 1.16+)
     *
     * @param config The YAML configuration to load from
     */
    public static void loadValuesFromConfig(YamlConfiguration config) {
        for (LogLevel level : values()) {
            // Skip migrate level
            String settingKey = level.getColourOption();
            if (settingKey == null) {
                continue;
            }

            // Load the colour from the config
            String value = config.getString(settingKey);
            if (value == null || value.isEmpty()) {
                continue;
            }

            try {
                Object parsedColor = parseColor(value);
                if (parsedColor != null) {
                    level.setColour(parsedColor);
                } else {
                    Logger.getLogger("Minecraft").warning(
                        String.format("Invalid color value for log level %s: %s (using default)", level, value)
                    );
                }
            } catch (Exception e) {
                Logger.getLogger("Minecraft").severe(
                    String.format("Exception occurred on log level loading (%s): %s", level, e.getMessage())
                );
            }
        }
    }

    /**
     * Parses a color string to ChatColor or hex string.
     * Supports:
     * - Traditional ChatColor names (e.g., "RED", "GREEN", "DARK_BLUE")
     * - HEX format: #RRGGBB (e.g., "#FF0000" for red)
     * - HEX format with ampersand: &#RRGGBB (e.g., "&#FF0000" for red)
     *
     * @param value The color string to parse
     * @return The parsed ChatColor or hex color string, or null if parsing fails
     */
    public static Object parseColor(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Trim whitespace
        value = value.trim();

        // Check if it's a HEX color with ampersand (&#RRGGBB)
        if (HEX_PATTERN_WITH_AMP.matcher(value).matches()) {
            return parseHexColor(value.substring(1)); // Remove the & prefix
        }

        // Check if it's a HEX color (#RRGGBB)
        if (HEX_PATTERN.matcher(value).matches()) {
            return parseHexColor(value);
        }

        // Try traditional ChatColor name
        try {
            return ChatColor.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Not a valid ChatColor name
            return null;
        }
    }

    /**
     * Parses a HEX color string and returns either the Minecraft hex format string
     * or a fallback ChatColor for older servers.
     *
     * @param hex The HEX color string (with or without # prefix)
     * @return The hex color string in Minecraft format, or a fallback ChatColor
     */
    private static Object parseHexColor(String hex) {
        // Ensure hex starts with #
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }

        // Validate hex format
        if (!hex.matches("#[A-Fa-f0-9]{6}")) {
            return null;
        }

        // Check if HEX colors are supported (1.16+)
        if (!supportsHexColors()) {
            // Fallback to closest traditional color for older versions
            return getClosestChatColor(hex);
        }

        // Convert hex to Minecraft format: §x§R§R§G§G§B§B
        return translateHexToMinecraftFormat(hex);
    }

    /**
     * Translates a HEX color to Minecraft's internal format.
     * Example: #FF0000 -> §x§F§F§0§0§0§0
     *
     * @param hex The HEX color string (must start with #)
     * @return The Minecraft formatted color string
     */
    private static String translateHexToMinecraftFormat(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        StringBuilder result = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            result.append("§").append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    /**
     * Checks if the server supports HEX colors (Minecraft 1.16+)
     * Uses a safe check that doesn't rely on ChatColor.of()
     *
     * @return true if HEX colors are supported
     */
    public static boolean supportsHexColors() {
        try {
            // Try to access a method that only exists in 1.16+
            // We use reflection to avoid compilation errors
            Class<?> chatColorClass = ChatColor.class;
            chatColorClass.getMethod("of", String.class);
            return true;
        } catch (NoSuchMethodException | SecurityException e) {
            return false;
        }
    }

    /**
     * Gets the closest traditional ChatColor to a HEX color.
     * Used as fallback for servers below 1.16.
     *
     * @param hex The HEX color string (e.g., "#FF0000")
     * @return The closest matching ChatColor
     */
    private static ChatColor getClosestChatColor(String hex) {
        if (hex == null) {
            return ChatColor.WHITE;
        }

        // Parse hex to RGB
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        if (hex.length() != 6) {
            return ChatColor.WHITE;
        }

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            return findClosestColor(r, g, b);
        } catch (NumberFormatException e) {
            return ChatColor.WHITE;
        }
    }

    /**
     * Finds the closest traditional ChatColor to the given RGB values.
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return The closest matching ChatColor
     */
    private static ChatColor findClosestColor(int r, int g, int b) {
        ChatColor closest = ChatColor.WHITE;
        double closestDistance = Double.MAX_VALUE;

        // Map of traditional ChatColors to their RGB values
        // These are the standard Minecraft colors
        ChatColor[] colors = {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN,
            ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_PURPLE,
            ChatColor.GOLD, ChatColor.GRAY, ChatColor.DARK_GRAY,
            ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
        };

        int[][] rgbValues = {
            {0, 0, 0},           // BLACK
            {0, 0, 170},         // DARK_BLUE
            {0, 170, 0},         // DARK_GREEN
            {0, 170, 170},       // DARK_AQUA
            {170, 0, 0},         // DARK_RED
            {170, 0, 170},       // DARK_PURPLE
            {254, 170, 0},       // GOLD
            {170, 170, 170},     // GRAY
            {85, 85, 85},        // DARK_GRAY
            {85, 85, 255},       // BLUE
            {85, 255, 85},       // GREEN
            {85, 255, 255},      // AQUA
            {255, 85, 85},       // RED
            {255, 85, 255},      // LIGHT_PURPLE
            {255, 255, 85},      // YELLOW
            {255, 255, 255}      // WHITE
        };

        for (int i = 0; i < colors.length; i++) {
            double distance = getDistance(r, g, b, rgbValues[i][0], rgbValues[i][1], rgbValues[i][2]);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = colors[i];
            }
        }

        return closest;
    }

    /**
     * Calculates the Euclidean distance between two RGB colors.
     *
     * @param r1 First color red
     * @param g1 First color green
     * @param b1 First color blue
     * @param r2 Second color red
     * @param g2 Second color green
     * @param b2 Second color blue
     * @return The distance between the colors
     */
    private static double getDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
        return Math.sqrt(
            Math.pow(r1 - r2, 2) +
            Math.pow(g1 - g2, 2) +
            Math.pow(b1 - b2, 2)
        );
    }

    /**
     * Gets the color for this log level as a string.
     * This can be either a ChatColor toString() or a hex color code.
     *
     * @return The color string
     */
    public String getColour() {
        if (colour instanceof ChatColor) {
            return ((ChatColor) colour).toString();
        } else if (colour instanceof String) {
            return (String) colour;
        }
        return ChatColor.WHITE.toString();
    }

    /**
     * Sets the color for this log level.
     *
     * @param colour The color to set (ChatColor or String for hex)
     */
    private void setColour(Object colour) {
        this.colour = colour;
    }

    /**
     * Gets the configuration option key for this log level's color.
     *
     * @return The configuration key, or null if not configurable
     */
    public String getColourOption() {
        return colourOption;
    }
}
