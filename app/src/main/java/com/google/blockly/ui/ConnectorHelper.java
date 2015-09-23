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
     * Add a "Previous" connector to the block's draw path.
     * <p/>
     * The reference point for this connector is the top-left corner of the block (top-right corner
     * in RTL mode).
     *
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
     * Add a "Next" connector to the block's draw path.
     * <p/>
     * The reference point for this connector is the bottom-left corner of the block
     * (bottom-right corner in RTL mode).
     *
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
     * Add a Value input connector to the block's draw path.
     * <p/>
     * The reference point for this connector is the top-right corner of the block
     * (top-left corner in RTL mode).
     *
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
     * Add a Statement input connector to the block's draw path.
     * <p/>
     * The reference point for this connector is the top-right corner of the Statement input
     * (top-left corner in RTL mode).
     *
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
     * Add an "Output" connector to the block's draw path.
     * <p/>
     * The reference point for this connector is the bottom-left corner of the block
     * (bottom-right corner in RTL mode).
     *
     * @param blockStartX Horizontal base coordinate of the connector; this is the left-hand side of
     *                    the block (right-hand side in RTL mode).
     * @param blockBottom Vertical view coordinate of the bottom of the block.
     * @param rtlSign Sign of horizontal connector direction. In RTL mode, this is -1, otherwise +1.
     */
    static void addOutputConnectorToPath(Path path, int blockStartX, int blockBottom, int rtlSign) {
        int connectorX = blockStartX - rtlSign * SIZE_PERPENDICULAR;

        int y = blockBottom + OFFSET_FROM_CORNER + SIZE_PARALLEL;
        path.lineTo(blockStartX, y);
        path.lineTo(connectorX, y);

        y -= SIZE_PARALLEL;
        path.lineTo(connectorX, y);
        path.lineTo(blockStartX, y);
    }
}
