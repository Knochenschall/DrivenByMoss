// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2019
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.framework.daw;

import de.mossgrabers.framework.daw.data.IBrowserColumn;
import de.mossgrabers.framework.daw.data.IBrowserColumnItem;


/**
 * Abstrct implementation of a browser. Adds functions based on generic interfaces.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public abstract class AbstractBrowser implements IBrowser
{
    protected final int             numResults;
    protected final int             numFilterColumnEntries;
    protected final ICursorDevice   cursorDevice;
    protected IBrowserColumnItem [] resultData;
    protected IBrowserColumn []     columnData;
    protected int                   selectedFilterColumn = 0;


    /**
     * Constructor.
     *
     * @param cursorDevice The cursor device
     * @param numFilterColumnEntries The number of entries in a filter column page
     * @param numResults The number of entries in a results column page
     */
    public AbstractBrowser (final ICursorDevice cursorDevice, final int numFilterColumnEntries, final int numResults)
    {
        this.cursorDevice = cursorDevice;
        this.numFilterColumnEntries = numFilterColumnEntries;
        this.numResults = numResults;
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasPreviousContentType ()
    {
        return this.getSelectedContentTypeIndex () > 0;
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasNextContentType ()
    {
        return this.getSelectedContentTypeIndex () < this.getContentTypeNames ().length - 1;
    }


    /** {@inheritDoc} */
    @Override
    public void resetFilterColumn (final int column)
    {
        this.columnData[column].resetFilter ();
    }


    /** {@inheritDoc} */
    @Override
    public IBrowserColumn getFilterColumn (final int column)
    {
        return this.columnData[column];
    }


    /** {@inheritDoc} */
    @Override
    public int getFilterColumnCount ()
    {
        return this.columnData.length;
    }


    /** {@inheritDoc} */
    @Override
    public String [] getFilterColumnNames ()
    {
        final String [] names = new String [this.columnData.length];
        for (int i = 0; i < this.columnData.length; i++)
            names[i] = this.columnData[i].getName ();
        return names;
    }


    /** {@inheritDoc} */
    @Override
    public IBrowserColumnItem [] getResultColumnItems ()
    {
        return this.resultData;
    }


    /** {@inheritDoc} */
    @Override
    public void selectPreviousFilterItem (final int columnIndex)
    {
        this.columnData[columnIndex].selectPreviousItem ();
    }


    /** {@inheritDoc} */
    @Override
    public IBrowserColumn getSelectedFilterColumn ()
    {
        return this.columnData[this.selectedFilterColumn];
    }


    /** {@inheritDoc} */
    @Override
    public void selectPreviousFilterColumn ()
    {
        if (this.selectedFilterColumn > 0)
            this.selectedFilterColumn--;
    }


    /** {@inheritDoc} */
    @Override
    public void selectNextFilterColumn ()
    {
        if (this.selectedFilterColumn < this.columnData.length)
            this.selectedFilterColumn++;
    }


    /** {@inheritDoc} */
    @Override
    public void selectNextFilterItem (final int columnIndex)
    {
        this.columnData[columnIndex].selectNextItem ();
    }


    /** {@inheritDoc} */
    @Override
    public void previousFilterItemPage (final int columnIndex)
    {
        this.columnData[columnIndex].scrollItemPageUp ();

    }


    /** {@inheritDoc} */
    @Override
    public void nextFilterItemPage (final int columnIndex)
    {
        this.columnData[columnIndex].scrollItemPageDown ();
    }


    /** {@inheritDoc} */
    @Override
    public int getSelectedFilterItemIndex (final int columnIndex)
    {
        if (columnIndex < 0)
            return -1;
        return this.columnData[columnIndex].getCursorIndex ();
    }


    /**
     * Set the index of the selected filter item of a column.
     *
     * @param columnIndex The index of the column
     * @param index The index of the item
     */
    public void setSelectedFilterItemIndex (final int columnIndex, final int index)
    {
        this.columnData[columnIndex].setCursorIndex (index);
    }


    /**
     * Get the index of the selected result item.
     *
     * @return The index of the result
     */
    public int getSelectedResultIndex ()
    {
        for (int i = 0; i < this.numResults; i++)
        {
            if (this.resultData[i].isSelected ())
                return i;
        }
        return -1;
    }


    /** {@inheritDoc} */
    @Override
    public int getNumResults ()
    {
        return this.numResults;
    }


    /** {@inheritDoc} */
    @Override
    public int getNumFilterColumnEntries ()
    {
        return this.numFilterColumnEntries;
    }
}
