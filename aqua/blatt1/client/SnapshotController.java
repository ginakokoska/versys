package aqua.blatt1.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SnapshotController implements ActionListener {
	private final Component parent;

	public SnapshotController(Component parent) {
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TankModel x = new TankModel();
		x.initiateSnapshot();
	}
}
