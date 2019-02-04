package com.google.blockly.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Language Utils
 */
public class LangUtils {
    private LangUtils() {
    }

    /**
     * Current Language
     */
    private static String lang = "en";

    /**
     * Get Language
     * @return Language
     */
    public static String getLang() {
        return lang;
    }

    /**
     * Set Language
     * @param newLang New Language
     */
    public static void setLang(String newLang) {
        lang = newLang;
    }

    /**
     * A Map Containing Translations
     */
    private static Map<String, String> langMap = new HashMap<>();

    /**
     * Replace Translation String with localized text using Blockly Web's translation files.
     * @param value Input Value
     * @return Translated String
     */
    public static String interpolate(String value) {
        for (Map.Entry<String, String> entry : langMap.entrySet()) {
            while (value.contains(entry.getKey())) {
                value = value.replace(entry.getKey(), entry.getValue());
            }
        }
        return value;
    }

    /**
     * Generate Map of translations.
     * @param context Context
     */
    public static void generateLang(Context context) {
        generateLang(context, getLang());
    }

    /**
     * Generate Map of translations.
     * @param context Context
     */
    private static void generateLang(Context context, String lang) {
        if (!lang.equals("en")) {
            generateLang(context, "en");
        }

        // Attempt to override language string table. Missing values will default to English loaded above.
        StringBuilder out = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("msg/js/" + lang + ".js")));

            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String[] lines = out.toString().split("\n");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
            if (lines[i].startsWith("Blockly.Msg[\"")) {
                boolean alias = false;

                lines[i] = lines[i].substring("Blockly.Msg[\"".length());
                String name = lines[i].substring(0, lines[i].indexOf('"'));
                if (lines[i].contains("\"] = \"")) {
                    lines[i] = lines[i].substring(lines[i].indexOf("\"] = \"") + "\"] = \"".length() - 1);
                } else {
                    lines[i] = lines[i].substring(lines[i].indexOf("\"] = Blockly.Msg[\"") + "\"] = Blockly.Msg[\"".length() - 1);
                    alias = true;
                }
                String value = lines[i].substring(1, lines[i].lastIndexOf('"'));
                if (alias) {
                    if (langMap.get("%{BKY_" + value + "}") == null) {
                        throw new RuntimeException(value + " is not defined.");
                    } else {
                        value = langMap.get("%{BKY_" + value + "}");
                    }
                }
                if (value != null) {
                    langMap.put("%{BKY_" + name + "}", value);
                }
            }
        }
    }
}
