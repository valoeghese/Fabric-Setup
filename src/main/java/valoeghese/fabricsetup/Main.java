package valoeghese.fabricsetup;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class Main {
    public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setTitle("Fabric Setup");

		JPanel versions = new JPanel(new BorderLayout());
		versions.setBorder(new TitledBorder("Versions"));

		frame.pack();
		frame.setVisible(true);
	}
}
