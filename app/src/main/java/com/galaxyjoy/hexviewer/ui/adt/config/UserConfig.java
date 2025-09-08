/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Undo Redo command
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.adt.config;

public interface UserConfig {
    float getFontSize();

    int getRowHeight();

    boolean isRowHeightAuto();

    boolean isDataColumnNotDisplayed();
}
