package com.google.blockly.model;

import android.support.v4.util.ArrayMap;

import com.google.blockly.android.codegen.LanguageDefinition;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.android.ui.mutator.IfElseMutatorFragment;
import com.google.blockly.android.ui.mutator.ProcedureDefinitionMutatorFragment;
import com.google.blockly.model.mutator.IfElseMutator;
import com.google.blockly.model.mutator.MathIsDivisibleByMutator;
import com.google.blockly.model.mutator.ProcedureCallMutator;
import com.google.blockly.model.mutator.ProcedureDefinitionMutator;
import com.google.blockly.model.mutator.ProceduresIfReturnMutator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Constants class for all default block definitions and supporting files.
 */
public final class DefaultBlocks {
    public static final String VARIABLE_CATEGORY_NAME = "VARIABLE";
    public static final String PROCEDURE_CATEGORY_NAME = "PROCEDURE"; // User label "Functions"

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
    /** Path to block definitions for blocks related to procedures (user defined functions). */
    public static final String PROCEDURE_BLOCKS_PATH = "default/procedures.json";
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
                    PROCEDURE_BLOCKS_PATH,
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
            Mutator.Factory[] factories = {
                IfElseMutator.FACTORY,
                MathIsDivisibleByMutator.FACTORY,
                ProcedureCallMutator.CALLNORETURN_FACTORY,
                ProcedureCallMutator.CALLRETURN_FACTORY,
                ProcedureDefinitionMutator.DEFNORETURN_FACTORY,
                ProcedureDefinitionMutator.DEFRETURN_FACTORY,
                ProceduresIfReturnMutator.FACTORY
                // TODO: Put other Mutator.Factorys
            };
            Map<String, Mutator.Factory> temp = new ArrayMap<>(factories.length);
            for (Mutator.Factory factory : factories) {
                temp.put(factory.getMutatorId(), factory);
            }
            DEFAULT_MUTATORS = Collections.unmodifiableMap(temp);
        }
        return DEFAULT_MUTATORS;
    }

    public static Map<String, MutatorFragment.Factory> getMutatorUis() {
        if (DEFAULT_MUTATOR_UIS == null) {
            Map<String, MutatorFragment.Factory> temp = new ArrayMap<>();
            temp.put(IfElseMutator.MUTATOR_ID, IfElseMutatorFragment.FACTORY);
            temp.put(ProcedureDefinitionMutator.DEFNORETURN_MUTATOR_ID, // Statement function
                    ProcedureDefinitionMutatorFragment.FACTORY);
            temp.put(ProcedureDefinitionMutator.DEFRETURN_MUTATOR_ID, // Value function
                    ProcedureDefinitionMutatorFragment.FACTORY);
            DEFAULT_MUTATOR_UIS = Collections.unmodifiableMap(temp);
        }
        return DEFAULT_MUTATOR_UIS;
    }

    public static Map<String, CustomCategory> getToolboxCustomCategories(
            BlocklyController controller) {
        // Don't store this map, because of the reference to the controller.
        Map<String, CustomCategory> map = new ArrayMap<>(2);
        map.put(VARIABLE_CATEGORY_NAME, new VariableCustomCategory(controller));
        map.put(PROCEDURE_CATEGORY_NAME, new ProcedureCustomCategory(controller));
        return Collections.unmodifiableMap(map);
    }

    // Not for instantiation.
    private DefaultBlocks() {}
}
