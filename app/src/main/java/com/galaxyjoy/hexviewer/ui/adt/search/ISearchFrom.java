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
package com.galaxyjoy.hexviewer.ui.adt.search;

public interface ISearchFrom {
    /**
     * Test if we aren't from the hex view or the plain view.
     *
     * @return boolean
     */
    boolean isSearchNotFromHexView();
}
