package com.google.blockly.model;

/**
 * Id for a particular field on blocks of a particular type.
 */

public final class BlockTypeFieldName {
    private final String mBlockType;
    private final String mFieldName;

    public BlockTypeFieldName(String blockType, String fieldName) {
        mBlockType = blockType;
        mFieldName = fieldName;
    }

    /**
     * @return The block type id the field belongs to.
     */
    public String getBlockType() {
        return mBlockType;
    }

    /**
     * @return The name of the field within the blocks.
     */
    public String getFieldName() {
        return mFieldName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BlockTypeFieldName other = (BlockTypeFieldName) obj;

        if (mBlockType != null ? !mBlockType.equals(other.mBlockType) : other.mBlockType != null)
            return false;
        return mFieldName != null ? mFieldName.equals(other.mFieldName) : other.mFieldName == null;

    }

    @Override
    public int hashCode() {
        int result = mBlockType != null ? mBlockType.hashCode() : 0;
        result = 31 * result + (mFieldName != null ? mFieldName.hashCode() : 0);
        return result;
    }
}
