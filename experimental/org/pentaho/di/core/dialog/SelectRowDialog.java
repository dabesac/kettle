 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/

 

package org.pentaho.di.core.dialog;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.BaseStepDialog;

import be.ibridge.kettle.core.ColumnInfo;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import be.ibridge.kettle.core.WindowProperty;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.widget.TableView;


/**
 * Displays an ArrayList of rows in a TableView and allows you to select one.
 * 
 * @author Matt
 */
public class SelectRowDialog extends Dialog
{
	private Label        wlFields;
	private TableView    wFields;
	private FormData     fdFields;

	private Button wOK, wCancel;
	private Listener lsOK, lsCancel;

	private Shell         shell;
	private ArrayList     buffer;
	private Props         props;
	private String        title;
    
	private RowMetaAndData selection;
    private RowMetaInterface rowMeta;
	
    /**
     * 
     * @param parent
     * @param style
     * @param buf
     */
	public SelectRowDialog(Shell parent, int style, ArrayList buffer)
	{
		super(parent, style);
		this.buffer=buffer;
		props=Props.getInstance();
		
		selection = null;
	}
	
	public void setTitle(String title)
	{
		this.title=title;
	}

	public RowMetaAndData open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX);
 		props.setLook(shell);

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		if (title==null) title = Messages.getString("SelectRowDialog.Title");

		shell.setLayout(formLayout);
		shell.setText(title);
		
		int margin = Const.MARGIN;

        // Simply exit and close in case we don't have anything to edit or show
        //
		if (buffer==null || buffer.size()==0) return null;
		
        rowMeta = ((RowMetaAndData)buffer.get(0)).getRowMeta();
        
		int FieldsRows=buffer.size();
		
		ColumnInfo[] colinf=new ColumnInfo[buffer.size()];
		for (int i=0;i<rowMeta.size();i++)
		{
			ValueMetaInterface v=rowMeta.getValueMeta(i);
			colinf[i]=new ColumnInfo(v.getName(),  ColumnInfo.COLUMN_TYPE_TEXT,   false);
			colinf[i].setToolTip(v.toStringMeta());
            colinf[i].setReadOnly(true);
		}
		
		wFields=new TableView(shell, 
						      SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, 
						      colinf, 
						      FieldsRows,  
						      null,
							  props
						      );

		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(Messages.getString("System.Button.OK"));
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel"));

		BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel }, margin, null);

		fdFields=new FormData();
		fdFields.left   = new FormAttachment(0, 0);
		fdFields.top    = new FormAttachment(wlFields, margin);
		fdFields.right  = new FormAttachment(100, 0);
		fdFields.bottom = new FormAttachment(wOK, -margin);
		wFields.setLayoutData(fdFields);


		// Add listeners
		lsOK = new Listener() { public void handleEvent(Event e) { ok(); } };
		wOK.addListener(SWT.Selection, lsOK    );
		
		lsCancel = new Listener() { public void handleEvent(Event e) { close(); } };
		wCancel.addListener(SWT.Selection, lsCancel  );
		
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { close(); } } );

		getData();
		
		BaseStepDialog.setSize(shell);

		shell.open();

		while (!shell.isDisposed())
		{
			if (!display.readAndDispatch()) display.sleep();
		}
		return selection;
	}

	public void dispose()
	{
		props.setScreen(new WindowProperty(shell));
		shell.dispose();
	}
	
	/**
	 * Copy information from the input buffer to the dialog fields.
	 */ 
	private void getData()
	{
		for (int i=0;i<buffer.size();i++)
		{
			RowMetaAndData rowMetaAndData = (RowMetaAndData)buffer.get(i);
            RowMetaInterface rowMeta = rowMetaAndData.getRowMeta();
            Object[] rowData = rowMetaAndData.getData();
			
			for (int c=0;c<rowMeta.size();c++)
			{
				ValueMetaInterface v=rowMeta.getValueMeta(c);
				String show;
				
                    try
                    {
                        if (v.isNumeric()) 
                        {
                            show = v.getString(rowData[c]);
                        }
                        else
                        {
                            show = v.getString(rowData[c]);
                        }
                    }
                    catch (KettleValueException e)
                    {
                        show = "<conversion error>";
                    }
				wFields.table.getItem(i).setText(c+1, show);
			}
		}
		wFields.optWidth(true);
	}
	
	private void close()
	{
		selection=null;
		dispose();
	}
	
	private void ok()
	{
	    int idx[] = wFields.getSelectionIndices();
	    if (idx.length==0) return;
        
		selection=(RowMetaAndData)buffer.get(idx[0]);
		dispose();
	}	
}
