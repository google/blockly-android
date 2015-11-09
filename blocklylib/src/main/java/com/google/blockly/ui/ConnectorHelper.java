/*
 * Copyright  2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.ui;

import android.graphics.Path;

/**
 * Helper class for drawing block connectors.
 */
public class ConnectorHelper {
    // The offset between a connector and the closest corner, in dips.
    static final int OFFSET_FROM_CORNER = 20;
    // The size of a connector perpendicular to the block boundary, in dips.
    static final int SIZE_PERPENDICULAR = 20;
    // The size of a connector parallel to the block boundary, in dips.
    static final int SIZE_PARALLEL = 40;

    // The minimum width of a Statement input connector to the right of its fields.
    static final int STATEMENT_INPUT_INDENT_WIDTH = 2 * OFFSET_FROM_CORNER + SIZE_PARALLEL;
    // Height (i.e., thickness) of the bottom of a C-shaped Statement input connector.
    static final int STATEMENT_INPUT_BOTTOM_HEIGHT = SIZE_PERPENDICULAR;

    // Width of an open (unconnected) inline Value input connector.
    static final int OPEN_INLINE_CONNECTOR_WIDTH = 80;
    // Height of an open (unconnected) inline Value input connector.
    static final int OPEN_INLINE_CONNECTOR_HEIGHT = 80;

    // Draw paths for standalone connectors. These are created when they are first needed, e.g., for
    // drawing a highlighted port. There are two paths for each type: one for RTL mode and one for
    // LTR mode.
    private static final Path[] mNextConnectorPath = new Path[]{
            createNextConnectorPath(-1), createNextConnectorPath(+1)
    };
    private static final Path[] mPreviousConnectorPath = new Path[]{
            createPreviousConnectorPath(-1), createPreviousConnectorPath(+1)
    };
    private static final Path[] mOutputConnectorPath = new Path[]{
            createOutputConnectorPath(-1), createOutputConnectorPath(+1)
    };
    private static final Path[] mValueInputConnectorPath = new Path[]{
            createValueInputConnectorPath(-1), createValueInputConnectorPath(+1)
    };

    /**
     * Add a "Previous" connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the top-left corner of the block (top-right corner
     * in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     * the block (right-hand side in RTL mode).
     * @param blockTop Vertical view coordinate of the top of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addPreviousConnectorToPath(Path path, int blockStartX, int blockTop, int rtlSign) {
        path.lineTo(blockStartX + rtlSign * OFFSET_FROM_CORNER, blockTop);
        path.rLineTo(0, SIZE_PERPENDICULAR);
        path.rLineTo(rtlSign * SIZE_PARALLEL, 0);
        path.rLineTo(0, -SIZE_PERPENDICULAR);
    }

    /**
     * Get a {@link Path} to draw a standalone Previous connector.
     */
    static Path getPreviousConnectorPath(int rtlSign) {
        return mPreviousConnectorPath[(rtlSign + 1) / 2];
    }

    /**
     * Add a "Next" connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the bottom-left corner of the block
     * (bottom-right corner in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     * the block (right-hand side in RTL mode).
     * @param blockBottom Vertical view coordinate of the bottom of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addNextConnectorToPath(Path path, int blockStartX, int blockBottom, int rtlSign) {
        path.lineTo(blockStartX + rtlSign * (OFFSET_FROM_CORNER + SIZE_PARALLEL), blockBottom);
        path.rLineTo(0, SIZE_PERPENDICULAR);
        path.rLineTo(-rtlSign * SIZE_PARALLEL, 0);
        path.rLineTo(0, -SIZE_PERPENDICULAR);
    }

    /**
     * Get a {@link Path} to draw a standalone Next connector.
     */
    static Path getNextConnectorPath(int rtlSign) {
        return mNextConnectorPath[(rtlSign + 1) / 2];
    }

    /**
     * Add a Value input connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the top-right corner of the block
     * (top-left corner in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockEndX Horizontal base coordinate of the connector; this is the right-hand side of
     * the block (left-hand side in RTL mode).
     * @param inputTop Vertical view coordinate of the top of the input for which this connector is
     * drawn.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addValueInputConnectorToPath(Path path, int blockEndX, int inputTop, int rtlSign) {
        path.lineTo(blockEndX, inputTop + OFFSET_FROM_CORNER);
        path.rLineTo(-rtlSign * SIZE_PERPENDICULAR, 0);
        path.rLineTo(0, SIZE_PARALLEL);
        path.rLineTo(rtlSign * SIZE_PERPENDICULAR, 0);
    }

    /**
     * Get a {@link Path} to draw a standalone value Input connector.
     */
    static Path getValueInputConnectorPath(int rtlSign) {
        return mValueInputConnectorPath[(rtlSign + 1) / 2];
    }

    /**
     * Add a Statement input connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the top-right corner of the Statement input
     * (top-left corner in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockEndAboveX Right-hand side of the block (left-hand side in RTL mode) above the
     * Statement connector.
     * @param blockEndBelowX Right-hand side of the block (left-hand side in RTL mode) below the
     * Statement connector. For inline inputs, this can be different from
     * {@code blockEndAboveX}.
     * @param inputTop Vertical view coordinate of the top of the InputView for which this connector
     * is drawn.
     * @param offsetX The offset of the Statement input connector from the left (or right, in RTL
     * mode) boundary of the block.
     * @param inputHeight The height of the connected input block(s).
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addStatementInputConnectorToPath(
            Path path, int blockEndAboveX, int blockEndBelowX, int inputTop, int offsetX,
            int inputHeight, int rtlSign) {
        // Draw to block-edge corner of C-shaped connector top.
        path.lineTo(blockEndAboveX, inputTop);

        // Draw "Next" connection.
        path.lineTo(offsetX + rtlSign * (OFFSET_FROM_CORNER + SIZE_PARALLEL), inputTop);
        path.rLineTo(0, SIZE_PERPENDICULAR);
        path.rLineTo(-rtlSign * SIZE_PARALLEL, 0);
        path.rLineTo(0, -SIZE_PERPENDICULAR);

        // Draw left-hand side and bottom of C-shaped connector.
        path.lineTo(offsetX, inputTop);
        path.lineTo(offsetX, inputTop + inputHeight);
        path.lineTo(blockEndBelowX, inputTop + inputHeight);
    }

    /**
     * Add an "Output" connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the bottom-left corner of the block
     * (bottom-right corner in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     * the block (right-hand side in RTL mode).
     * @param blockTop Vertical view coordinate of the top of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addOutputConnectorToPath(Path path, int blockStartX, int blockTop, int rtlSign) {
        path.lineTo(blockStartX, blockTop + OFFSET_FROM_CORNER + SIZE_PARALLEL);
        path.rLineTo(-rtlSign * SIZE_PERPENDICULAR, 0);
        path.rLineTo(0, -SIZE_PARALLEL);
        path.rLineTo(rtlSign * SIZE_PERPENDICULAR, 0);
    }

    /**
     * Get a {@link Path} to draw a standalone Output connector.
     */
    static Path getOutputConnectorPath(int rtlSign) {
        return mOutputConnectorPath[(rtlSign + 1) / 2];
    }

    /**
     * Create a {@link Path} to draw a standalone Previous connector.
     */
    private static Path createPreviousConnectorPath(int rtlSign) {
        Path path = new Path();
        addPreviousConnectorToPath(path, 0, 0, rtlSign);
        path.lineTo(rtlSign * (SIZE_PARALLEL + 2 * OFFSET_FROM_CORNER), 0);
        return path;
    }

    /**
     * Create a {@link Path} to draw a standalone Next connector.
     */
    private static Path createNextConnectorPath(int rtlSign) {
        Path path = new Path();
        path.moveTo(rtlSign * (SIZE_PARALLEL + 2 * OFFSET_FROM_CORNER), 0);
        addNextConnectorToPath(path, 0, 0, rtlSign);
        path.lineTo(0, 0);
        return path;
    }

    /**
     * Create a {@link Path} to draw a standalone value Input connector.
     */
    private static Path createValueInputConnectorPath(int rtlSign) {
        Path path = new Path();
        addValueInputConnectorToPath(path, 0, 0, rtlSign);
        path.lineTo(0, 2 * OFFSET_FROM_CORNER + SIZE_PARALLEL);
        return path;
    }

    /**
     * Create a {@link Path} to draw a standalone Output connector.
     */
    private static Path createOutputConnectorPath(int rtlSign) {
        Path path = new Path();
        path.moveTo(0, 2 * OFFSET_FROM_CORNER + SIZE_PARALLEL);
        addOutputConnectorToPath(path, 0, 0, rtlSign);
        path.lineTo(0, 0);
        return path;
    }
}
