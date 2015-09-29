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

    /**
     * Add a "Previous" connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the top-left corner of the block (top-right corner
     * in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     *                    the block (right-hand side in RTL mode).
     * @param blockTop Vertical view coordinate of the top of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addPreviousConnectorToPath(Path path, int blockStartX, int blockTop, int rtlSign) {
        int x = blockStartX + rtlSign * OFFSET_FROM_CORNER;
        path.lineTo(x, blockTop);
        path.lineTo(x, blockTop + SIZE_PERPENDICULAR);

        x += rtlSign * SIZE_PARALLEL;
        path.lineTo(x, blockTop + SIZE_PERPENDICULAR);
        path.lineTo(x, blockTop);
    }

    /**
     * Create a {@link Path} to draw a "Previous" connector.
     *
     * @param path This path is cleared by calling {@code rewind()}, after which commands to draw
     *             the connector are added to it.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     *                    the block (right-hand side in RTL mode).
     * @param blockTop Vertical view coordinate of the top of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void createPreviousConnectorPath(Path path, int blockStartX, int blockTop, int rtlSign) {
        path.rewind();
        path.moveTo(blockStartX, blockTop);
        addPreviousConnectorToPath(path, blockStartX, blockTop, rtlSign);
        path.lineTo(blockStartX + rtlSign * (SIZE_PARALLEL + 2 * OFFSET_FROM_CORNER), blockTop);
    }

    /**
     * Add a "Next" connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the bottom-left corner of the block
     * (bottom-right corner in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     *                    the block (right-hand side in RTL mode).
     * @param blockBottom Vertical view coordinate of the bottom of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addNextConnectorToPath(Path path, int blockStartX, int blockBottom, int rtlSign) {
        int x = blockStartX + rtlSign * (OFFSET_FROM_CORNER + SIZE_PARALLEL);
        path.lineTo(x, blockBottom);
        path.lineTo(x, blockBottom + SIZE_PERPENDICULAR);

        x -= rtlSign * SIZE_PARALLEL;
        path.lineTo(x, blockBottom + SIZE_PERPENDICULAR);
        path.lineTo(x, blockBottom);
    }

    /**
     * Create a {@link Path} to draw a Next connector.
     *
     * @param path This path is cleared by calling {@code rewind()}, after which commands to draw
     *             the connector are added to it.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     *                    the block (right-hand side in RTL mode).
     * @param blockBottom Vertical view coordinate of the bottom of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void createNextConnectorPath(Path path, int blockStartX, int blockBottom, int rtlSign) {
        path.rewind();
        path.moveTo(blockStartX + rtlSign * (SIZE_PARALLEL + 2 * OFFSET_FROM_CORNER), blockBottom);
        addNextConnectorToPath(path, blockStartX, blockBottom, rtlSign);
        path.lineTo(blockStartX, blockBottom);
    }

    /**
     * Add a Value input connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the top-right corner of the block
     * (top-left corner in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockEndX Horizontal base coordinate of the connector; this is the right-hand side of
     *                  the block (left-hand side in RTL mode).
     * @param inputTop Vertical view coordinate of the top of the input for which this connector is
     *                 drawn.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addValueInputConnectorToPath(Path path, int blockEndX, int inputTop, int rtlSign) {
        int connectorX = blockEndX - rtlSign * SIZE_PERPENDICULAR;

        int y = inputTop + OFFSET_FROM_CORNER;
        path.lineTo(blockEndX, y);
        path.lineTo(connectorX, y);

        y += SIZE_PARALLEL;
        path.lineTo(connectorX, y);
        path.lineTo(blockEndX, y);
    }

    /**
     * Create a path to draw only a Value input connector.
     *
     * @param blockEndX Horizontal base coordinate of the connector; this is the right-hand side of
     *                  the block (left-hand side in RTL mode).
     * @param inputTop Vertical view coordinate of the top of the input for which this connector is
     *                 drawn.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void createValueInputConnectorPath(Path path, int blockEndX, int inputTop, int rtlSign) {
        path.rewind();
        path.moveTo(blockEndX, inputTop);
        addValueInputConnectorToPath(path, blockEndX, inputTop, rtlSign);
        path.lineTo(blockEndX, inputTop + SIZE_PARALLEL + 2 * OFFSET_FROM_CORNER);
    }

    /**
     * Add a Statement input connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the top-right corner of the Statement input
     * (top-left corner in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockEndAboveX Right-hand side of the block (left-hand side in RTL mode) above the
     *                       Statement connector.
     * @param blockEndBelowX Right-hand side of the block (left-hand side in RTL mode) below the
     *                       Statement connector. For inline inputs, this can be different from
     *                       {@code blockEndAboveX}.
     * @param inputTop Vertical view coordinate of the top of the InputView for which this connector
     *                 is drawn.
     * @param offsetX The offset of the Statement input connector from the left (or right, in RTL
     *                mode) boundary of the block.
     * @param inputHeight The height of the connected input block(s).
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addStatementInputConnectorToPath(
            Path path, int blockEndAboveX, int blockEndBelowX, int inputTop, int offsetX,
            int inputHeight, int rtlSign) {
        path.lineTo(blockEndAboveX, inputTop);

        int x = offsetX + rtlSign * (OFFSET_FROM_CORNER + SIZE_PARALLEL);
        path.lineTo(x, inputTop);
        path.lineTo(x, inputTop + SIZE_PERPENDICULAR);

        x -= rtlSign * SIZE_PARALLEL;
        path.lineTo(x, inputTop + SIZE_PERPENDICULAR);
        path.lineTo(x, inputTop);

        path.lineTo(offsetX, inputTop);
        path.lineTo(offsetX, inputTop + inputHeight);
        path.lineTo(blockEndBelowX, inputTop + inputHeight);
    }

    /**
     * Create a {@link Path} to draw only a Statement input connector.
     * <p/>
     * Specifically, this method adds the drawing commands for the optinally-highlighted part of the
     * connector, but <em>not</em> the entire C-shaped opening for child blocks.
     *
     * @param path This path is cleared by calling {@code rewind()}, after which commands to draw
     *             the connector are added to it.
     * @param blockEndAboveX Right-hand side of the block (left-hand side in RTL mode) above the
     *                       Statement connector.
     * @param blockEndBelowX Right-hand side of the block (left-hand side in RTL mode) below the
     *                       Statement connector. For inline inputs, this can be different from
     *                       {@code blockEndAboveX}.
     * @param inputTop Vertical view coordinate of the top of the InputView for which this connector
     *                 is drawn.
     * @param offsetX  The offset of the Statement input connector from the left (or right, in RTL
     *                 mode) boundary of the block.
     * @param inputHeight The height of the connected input block(s).
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void createStatementInputConnectorPath(
            Path path, int blockEndAboveX, int blockEndBelowX, int inputTop, int offsetX,
            int inputHeight, int rtlSign) {
        path.rewind();

        int x = offsetX + rtlSign * (2 * OFFSET_FROM_CORNER + SIZE_PARALLEL);
        path.moveTo(x, inputTop);

        x -= rtlSign * OFFSET_FROM_CORNER;
        path.lineTo(x, inputTop);
        path.lineTo(x, inputTop + SIZE_PERPENDICULAR);

        x -= rtlSign * SIZE_PARALLEL;
        path.lineTo(x, inputTop + SIZE_PERPENDICULAR);
        path.lineTo(x, inputTop);

        path.lineTo(offsetX, inputTop);
    }

    /**
     * Add an "Output" connector to an existing {@link Path}.
     * <p/>
     * The reference point for this connector is the bottom-left corner of the block
     * (bottom-right corner in RTL mode).
     *
     * @param path Drawing commands for the connector are added to this path.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     *                    the block (right-hand side in RTL mode).
     * @param blockTop Vertical view coordinate of the top of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addOutputConnectorToPath(Path path, int blockStartX, int blockTop, int rtlSign) {
        int connectorX = blockStartX - rtlSign * SIZE_PERPENDICULAR;

        int y = blockTop + OFFSET_FROM_CORNER + SIZE_PARALLEL;
        path.lineTo(blockStartX, y);
        path.lineTo(connectorX, y);

        y -= SIZE_PARALLEL;
        path.lineTo(connectorX, y);
        path.lineTo(blockStartX, y);
    }

    /**
     * Create a path to draw only an "Output" connector.
     *
     * @param path This path is cleared by calling {@code rewind()}, after which commands to draw
     *             the connector are added to it.
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     *                    the block (right-hand side in RTL mode).
     * @param blockTop Vertical view coordinate of the top of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void createOutputConnectorPath(Path path, int blockStartX, int blockTop, int rtlSign) {
        path.rewind();
        path.moveTo(blockStartX, blockTop + 2 * OFFSET_FROM_CORNER + SIZE_PARALLEL);
        addOutputConnectorToPath(path, blockStartX, blockTop, rtlSign);
        path.lineTo(blockStartX, blockTop);
    }

    /**
     * Get the point that will be used as the position of the connection when finding neighbors,
     * bumping, etc.  Since previous and next connections are in the same location when connected,
     * the calculations are the same for both.
     *
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     *      the block (right-hand side in RTL mode).
     * @param blockY Vertical base coordinate of the connector; this is the bottom of the block for
     *      a next connection and the top of the block for a previous connection.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     * @param toUpdate A {@link} ViewPoint to modify with the coordinates.
     */
    static void getNextOrPreviousConnectionPosition(int blockStartX, int blockY, int rtlSign,
                                             ViewPoint toUpdate) {
        int x = blockStartX + rtlSign * (OFFSET_FROM_CORNER + SIZE_PARALLEL / 2);
        int y = blockY + SIZE_PERPENDICULAR / 2;
        toUpdate.set(x, y);
    }

    /**
     * Get the point that will be used as the position of the connection when finding neighbors,
     * bumping, etc.  Since value inputs and outputs are in the same location when connected,
     * the calculations are the same for both.
     *
     * @param blockX Horizontal base coordinate of the connector; for an input this is the
     *              right-hand side of the block (left-hand side in RTL mode).  For an output it is
     *              the left-hand side of the block (right-hand side in RTL mode).
     * @param inputTop Vertical view coordinate of the top of the input for which this connector is
     *                 drawn.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     * @param toUpdate A {@link} ViewPoint to modify with the coordinates.
     */
    static void getOutputOrValueInputConnectionPosition(int blockX, int inputTop, int rtlSign,
                                             ViewPoint toUpdate) {
        int x = blockX - rtlSign * (SIZE_PERPENDICULAR / 2);
        int y = inputTop + OFFSET_FROM_CORNER + (SIZE_PARALLEL / 2);
        toUpdate.set(x, y);
    }

    /**
     * Get the point that will be used as the position of the connection when finding neighbors,
     * bumping, etc.
     *
     * @param inputTop Vertical view coordinate of the top of the InputView for which this connector
     *                 is drawn.
     * @param offsetX The offset of the Statement input connector from the left (or right, in RTL
     *                mode) boundary of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     * @param toUpdate A {@link} ViewPoint to modify with the coordinates.
     */
    static void getStatementInputConnectionPosition(int inputTop, int offsetX, int rtlSign,
                                               ViewPoint toUpdate) {
        int x = offsetX + rtlSign * (OFFSET_FROM_CORNER + SIZE_PARALLEL / 2);
        int y = inputTop + SIZE_PERPENDICULAR / 2;
        toUpdate.set(x, y);
    }
}
