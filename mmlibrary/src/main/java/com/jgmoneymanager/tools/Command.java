package com.jgmoneymanager.tools;

import android.content.DialogInterface;

/**
 * Functor object that allows us to execute arbitrary code.
 * 
 * This is used in conjunction with dialog boxes to allow us to execute any
 * actions we like when a button is pressed in a dialog box (dialog boxes are no
 * longer blocking, meaning we need to register listeners for the various
 * buttons of the dialog instead of waiting for the result)
 * 
 * @author NDUNN
 * 
 */
public interface Command {
	public void execute();

	public static final Command NO_OP = new Command() {
		public void execute() {
		}
	};

	public static class CommandWrapper implements
			DialogInterface.OnClickListener {
		private final Command command;

		public CommandWrapper(Command command) {
			this.command = command;
		}

		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			command.execute();
		}

		public void execute() {
			command.execute();
		}
	}

}
