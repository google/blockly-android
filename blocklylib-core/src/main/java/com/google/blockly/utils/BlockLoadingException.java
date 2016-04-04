package com.google.blockly.utils;

import java.io.IOException;

/**
 * Exception thrown when Blockly encounters an error loading blocks.
 */
public class BlockLoadingException extends IOException {
    public BlockLoadingException(String mesg) {
        super(mesg);
    }

    public BlockLoadingException(Throwable cause) {
        super(cause);
    }

    public BlockLoadingException(String mesg, Throwable cause) {
        super(mesg, cause);
    }
}
