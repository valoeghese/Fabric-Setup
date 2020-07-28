package valoeghese.fabricsetup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import tk.valoeghese.zoesteriaconfig.api.container.WritableConfig;
import tk.valoeghese.zoesteriaconfig.impl.parser.ImplZoesteriaDefaultDeserialiser;

public class Main {
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		try {
			frame.setTitle("Fabric Setup");

			JPanel versions = new JPanel(new BorderLayout());
			versions.setBorder(new TitledBorder("Version"));

			frame.pack();
			frame.setVisible(true);
		} catch (Throwable t) {
			// I ripped this whole catch block from a swing ccg I'm making
			t.printStackTrace();

			StringBuilder sb = new StringBuilder();
			t.printStackTrace(new PrintStream(new StringOutputStream(sb)));

			JPanel container = new JPanel(new BorderLayout(0, 10));
			JTextArea error = makeWrapping(new JLabel("The following error occurred."));

			Font font = new Font(error.getFont().getName(), Font.BOLD, 15);
			error.setFont(font);

			container.add(error, BorderLayout.NORTH);

			JTextArea crashReport = new JTextArea();
			crashReport.setEditable(false);
			crashReport.append(sb.toString());

			JScrollPane scroll = new JScrollPane(
					crashReport,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroll.setPreferredSize(new Dimension(600, 300));

			container.add(scroll, BorderLayout.CENTER);

			JDialog dialog = new JDialog(frame, "The Game has Crashed!", true);
			dialog.setContentPane(container);
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
				}
			});
			dialog.pack();
			dialog.setVisible(true);
		}
	}

	private static WritableConfig getOnlineOrLocal(String name) {
		String result;

		try {
			URL url = new URL("https://raw.githubusercontent.com/valoeghese/Fabric-Setup/master/src/main/resources/" + name);
			result = ResourceLoader.readString(url::openStream);
		} catch (IOException e) {
			System.out.println("Likely no internet while retrieving online file for " + name + ", reverting to local copy.");

			try {
				result = ResourceLoader.loadAsString(name);
			} catch (IOException e1) {
				throw new UncheckedIOException(e1);
			}
		}

		return new StringZFGParser<>(result, new ImplZoesteriaDefaultDeserialiser(true)).asWritableConfig();
	}

	// wow thanks stackoverflow
	private static JTextArea makeWrapping(JLabel label) {
		JTextArea result = new JTextArea();
		result.setText(label.getText());
		result.setWrapStyleWord(true);
		result.setLineWrap(true);
		result.setOpaque(false);
		result.setEditable(false);
		result.setFocusable(false);
		result.setBackground(UIManager.getColor("Label.background"));
		result.setFont(UIManager.getFont("Label.font"));
		result.setBorder(UIManager.getBorder("Label.border"));
		return result;
	}

	private static class StringOutputStream extends OutputStream {
		public StringOutputStream(StringBuilder sb) {
			this.sb = sb;
		}

		private final StringBuilder sb;

		@Override
		public void write(int b) throws IOException {
			this.sb.append((char) b);
		}
	}
}
