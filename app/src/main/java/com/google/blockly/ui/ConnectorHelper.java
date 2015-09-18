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
    // The size of a connector perpendicular to the block boundary, in dips.
    static final int CONNECTOR_SIZE_PERPENDICULAR = 20;
    // The offset between a connector and the closest corner, in dips.
    static final int CONNECTOR_OFFSET = 20;
    // The size of a connector parallel to the block boundary, in dips.
    static final int CONNECTOR_SIZE_PARALLEL = 40;

    // The minimum width of a Statement input connector to the right of its fields.
    static final int STATEMENT_INPUT_MINIMUM_WIDTH = 4 * CONNECTOR_SIZE_PARALLEL;
    // Height (i.e., thickness) of the bottom of a Statement input connector.
    static final int STATEMENT_INPUT_BOTTOM_HEIGHT = CONNECTOR_SIZE_PERPENDICULAR;

    static final int OPEN_INLINE_CONNECTOR_WIDTH = 80;
    static final int OPEN_INLINE_CONNECTOR_HEIGHT = 80;

    /**
     * Add a "Previous" connector to the block's draw path.
     *
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     */
    static void addPreviousConnectorToPath(Path path, int xFrom, int yFrom) {
        int x = xFrom + CONNECTOR_OFFSET;
        path.lineTo(x, yFrom);
        path.lineTo(x, yFrom + CONNECTOR_SIZE_PERPENDICULAR);

        x += CONNECTOR_SIZE_PARALLEL;
        path.lineTo(x, yFrom + CONNECTOR_SIZE_PERPENDICULAR);
        path.lineTo(x, yFrom);
    }

    /**
     * Add a "Next" connector to the block's draw path.
     *
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     */
    static void addNextConnectorToPath(Path path, int xFrom, int yFrom) {
        int x = xFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL;
        path.lineTo(x, yFrom);
        path.lineTo(x, yFrom + CONNECTOR_SIZE_PERPENDICULAR);

        x -= CONNECTOR_SIZE_PARALLEL;
        path.lineTo(x, yFrom + CONNECTOR_SIZE_PERPENDICULAR);
        path.lineTo(x, yFrom);
    }

    /**
     * Add a Value input connector to the block's draw path.
     *
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     */
    static void addValueInputConnectorToPath(Path path, int xFrom, int yFrom) {
        int y = yFrom + CONNECTOR_OFFSET;
        path.lineTo(xFrom, y);
        path.lineTo(xFrom - CONNECTOR_SIZE_PERPENDICULAR, y);

        y +=  CONNECTOR_SIZE_PARALLEL;
        path.lineTo(xFrom - CONNECTOR_SIZE_PERPENDICULAR, y);
        path.lineTo(xFrom, y);
    }

    /**
     * Add a Statement input connector to the block's draw path.
     *
     * @param xFrom   Horizontal view coordinate of the reference point for this connector.
     * @param yFrom   Vertical view coordinate of the reference point for this connector.
     * @param xOffset The offset of the Statement input connector from the left (or right, in RTL
     *                mode) boundary of the block.
     */
    static void addStatementInputConnectorToPath(
            Path path, int xFrom, int yFrom, int xOffset, int connectorHeight) {
        path.lineTo(xFrom, yFrom);

        int x = xOffset + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL;
        path.lineTo(x, yFrom);
        path.lineTo(x, yFrom + CONNECTOR_SIZE_PERPENDICULAR);

        x -= CONNECTOR_SIZE_PARALLEL;
        path.lineTo(x, yFrom + CONNECTOR_SIZE_PERPENDICULAR);
        path.lineTo(x, yFrom);

        path.lineTo(xOffset, yFrom);
        path.lineTo(xOffset, yFrom + connectorHeight);
        path.lineTo(xFrom, yFrom + connectorHeight);
    }

    /**
     * Add a "Output" connector to the block's draw path.
     *
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     */
    static void addOutputConnectorToPath(Path path, int xFrom, int yFrom) {
        int y = yFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL;
        path.lineTo(xFrom, y);
        path.lineTo(xFrom - CONNECTOR_SIZE_PERPENDICULAR, y);

        y -= CONNECTOR_SIZE_PARALLEL;
        path.lineTo(xFrom - CONNECTOR_SIZE_PERPENDICULAR, y);
        path.lineTo(xFrom, y);
    }
}
