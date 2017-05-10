package com.google.blockly.model;

import android.content.Context;
import android.support.v4.util.ArrayMap;

import com.google.blockly.android.codegen.LanguageDefinition;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.android.ui.mutator.IfElseMutatorFragment;
import com.google.blockly.model.mutator.IfElseMutator;
import com.google.blockly.model.mutator.MathIsDivisibleByMutator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Constants class for all default block definitions and supporting files.
 */
public final class DefaultBlocks {
    /** Path to block definitions for blocks related to colors. */
    public static final String COLOR_BLOCKS_PATH = "default/colour_blocks.json";
    /** Path to block definitions for blocks related to lists. Does not include math on lists. */
    public static final String LIST_BLOCKS_PATH = "default/list_blocks.json";
    /** Path to block definitions for blocks related to logic (if statements, etc.). */
    public static final String LOGIC_BLOCKS_PATH = "default/logic_blocks.json";
    /** Path to block definitions for blocks related to loops. */
    public static final String LOOP_BLOCKS_PATH = "default/loop_blocks.json";
    /** Path to block definitions for blocks related to math, including math on lists. */
    public static final String MATH_BLOCKS_PATH = "default/math_blocks.json";
    /** Path to block definitions for blocks related to strings/text. */
    public static final String TEXT_BLOCKS_PATH = "default/text_blocks.json";
    /** Path to block definitions for blocks related to variables. */
    public static final String VARIABLE_BLOCKS_PATH = "default/variable_blocks.json";

    /** Path to a toolbox that has most of the default blocks organized into categories. */
    public static final String TOOLBOX_PATH = "default/toolbox.xml";

    /**
     * Default language definition uses the JavaScript language generator.
     */
    public static final LanguageDefinition LANGUAGE_DEFINITION
            = LanguageDefinition.JAVASCRIPT_LANGUAGE_DEFINITION;

    // Lazily constructed collections.
    private static List<String> ALL_BLOCK_DEFINITIONS = null;
    private static Map<String, BlockExtension> DEFAULT_EXTENSIONS  = null;
    private static Map<String, Mutator.Factory> DEFAULT_MUTATORS = null;
    private static Map<String, MutatorFragment.Factory> DEFAULT_MUTATOR_UIS = null;

    /**
     * Returns the default list of {@link BlockExtension}s. This list is loaded lazily, so it will
     * not load the Extension and related classes if never called.
     * @return The map of default extensions, keyed by extension id.
     */
    public static List<String> getAllBlockDefinitions() {
        if (ALL_BLOCK_DEFINITIONS == null) {
            ALL_BLOCK_DEFINITIONS = Collections.unmodifiableList(Arrays.asList(
                    COLOR_BLOCKS_PATH,
                    LIST_BLOCKS_PATH,
                    LOGIC_BLOCKS_PATH,
                    LOOP_BLOCKS_PATH,
                    MATH_BLOCKS_PATH,
                    TEXT_BLOCKS_PATH,
                    VARIABLE_BLOCKS_PATH
            ));
        }
        return ALL_BLOCK_DEFINITIONS;
    }

    /**
     * Returns the default list of {@link BlockExtension}s. This list is loaded lazily, so it will
     * not load the Extension and related classes if never called.
     * @return The map of default extensions, keyed by extension id.
     */
    public static Map<String, BlockExtension> getExtensions() {
        if (DEFAULT_EXTENSIONS == null) {
            Map<String, BlockExtension> temp = new ArrayMap<>();
            // TODO: Put new BlockExtensions
            DEFAULT_EXTENSIONS = Collections.unmodifiableMap(temp);
        }
        return DEFAULT_EXTENSIONS;
    }

    /**
     * Returns the default list of factories for the default {@link Mutator}s. This list is loaded
     * lazily, so it will not load the related classes if never called.
     * @return The map of factories for the default mutators, keyed by mutator id.
     *
     */
    public static Map<String, Mutator.Factory> getMutators() {
        if (DEFAULT_MUTATORS == null) {
            Map<String, Mutator.Factory> temp = new ArrayMap<>();
            temp.put(MathIsDivisibleByMutator.MUTATOR_ID, new MathIsDivisibleByMutator.Factory());
            temp.put(IfElseMutator.MUTATOR_ID, new IfElseMutator.Factory());
            // TODO: Put other Mutator.Factorys
            DEFAULT_MUTATORS = Collections.unmodifiableMap(temp);
        }
        return DEFAULT_MUTATORS;
    }

    public static Map<String, MutatorFragment.Factory> getMutatorUis(Context context,
            BlocklyController controller) {
        if (DEFAULT_MUTATOR_UIS == null) {
            Map<String, MutatorFragment.Factory> temp = new ArrayMap<>();
            temp.put(IfElseMutator.MUTATOR_ID, new IfElseMutatorFragment.Factory());
            DEFAULT_MUTATOR_UIS = Collections.unmodifiableMap(temp);
        }
        return DEFAULT_MUTATOR_UIS;
    }

    // Not for instantiation.
    private DefaultBlocks() {}
}
