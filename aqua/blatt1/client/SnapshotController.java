package aqua.blatt1.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SnapshotController implements ActionListener {
	private final TankModel tankModel;

	public SnapshotController(Component parent, TankModel tankModel) {
		this.tankModel = tankModel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TankModel x = new TankModel();
		x.initiateSnapshot();
	}
}
