package ru.valerchik.fyronsniffer.utils;

import org.jetbrains.annotations.Nullable;

public final class NumberParser {
    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static @Nullable Integer parsePositiveInteger(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static @Nullable Double parseChance(String value) {
        try {
            double parsed = Double.parseDouble(value.replace(',', '.'));
            return parsed >= 0.0D && parsed <= 100.0D ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
