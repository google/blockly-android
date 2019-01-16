package com.google.blockly.android.codegen;

/**
 * Defines the core language file to be used in code generation. To be used by the generator
 * Blockly needs to know the path to the file and the object that has the generator
 * functions. For example: {"javascript_compressed.js", "Blockly.JavaScript"}.
 */
// TODO (#378): Add the list of reserved keywords to the language definition.
public class LanguageDefinition {
    /**
     * Standard definition for the JavaScript language generator.
     */
    public final static LanguageDefinition JAVASCRIPT_LANGUAGE_DEFINITION
            = new LanguageDefinition("generators/javascript_compressed.js", "Blockly.JavaScript");

    /**
     * Standard definition for the Lua language generator. (Requires "full" flavor)
     */
    public final static LanguageDefinition LUA_LANGUAGE_DEFINITION
            = new LanguageDefinition("generators/lua_compressed.js", "Blockly.Lua");

    /**
     * Standard definition for the PHP language generator. (Requires "full" flavor)
     */
    public final static LanguageDefinition PHP_LANGUAGE_DEFINITION
            = new LanguageDefinition("generators/php_compressed.js", "Blockly.PHP");

    /**
     * Standard definition for the Python language generator. (Requires "full" flavor)
     */
    public final static LanguageDefinition PYTHON_LANGUAGE_DEFINITION
            = new LanguageDefinition("generators/python_compressed.js", "Blockly.Python");

    /**
     * Standard definition for the Dart language generator. (Requires "full" flavor)
     */
    public final static LanguageDefinition DART_LANGUAGE_DEFINITION
            = new LanguageDefinition("generators/dart_compressed.js", "Blockly.Dart");

    /**
     * The path to the language generation file relative to
     * file:///android_assets/background_compiler.html.
     */
    public final String mLanguageFilename;
    /**
     * The Generator object that is defined by the file and should be called to perform the code
     * generation, such as "Blockly.JavaScript".
     */
    public final String mGeneratorRef;

    /**
     * Create a language definition with the given filename and generator object.
     *
     * @param filename The path to the language generator file relative to
     *                 file:///android_assets/background_compiler.html.
     * @param generatorObject The generator object provided by the file, such as
     *                  "Blockly.JavaScript"
     */
    public LanguageDefinition(String filename, String generatorObject) {
        mLanguageFilename = filename;
        mGeneratorRef = generatorObject;
    }
}
