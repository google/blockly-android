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
            = new LanguageDefinition("javascript_compressed.js", "Blockly.JavaScript");

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
