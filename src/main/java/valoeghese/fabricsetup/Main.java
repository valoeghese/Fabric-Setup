package valoeghese.fabricsetup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import tk.valoeghese.zoesteriaconfig.api.container.WritableConfig;
import tk.valoeghese.zoesteriaconfig.impl.parser.ImplZoesteriaDefaultDeserialiser;

public class Main {
	public static void main(String[] args) throws Throwable {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		try {
			WritableConfig masterOptions = parseOnlineOrLocal("master.zfg");

			JPanel master = new JPanel(new BorderLayout());
			master.setPreferredSize(new Dimension(275, 350));

			// top
			JPanel overallInfo = new JPanel(new BorderLayout());

			JTextField modId = new JTextField();
			modId.setText("modid");
			modId.setBorder(new TitledBorder("Mod Id"));
			overallInfo.add(modId, BorderLayout.NORTH);
			JTextField mavenGroup = new JTextField();
			mavenGroup.setText("com.example");
			mavenGroup.setBorder(new TitledBorder("Maven Group"));
			overallInfo.add(mavenGroup, BorderLayout.SOUTH);

			master.add(overallInfo, BorderLayout.NORTH);

			// centre
			JPanel settings = new JPanel(new BorderLayout());
			settings.setBorder(new TitledBorder("Settings"));

			// Minecraft/Yarn Stuff
			JPanel pureMC = new JPanel(new BorderLayout());
			String[] vs = masterOptions.getList("versions").toArray(new String[0]);
			JComboBox<String> minecraftVersion = new JComboBox<>(vs);
			minecraftVersion.setBorder(new TitledBorder("Minecraft Version"));
			pureMC.add(minecraftVersion, BorderLayout.NORTH);

			JTextField yarnBuild = new JTextField();
			yarnBuild.setText(masterOptions.getStringValue(vs[0].replace('.', '-') + ".yarn_latest"));
			pureMC.add(yarnBuild, BorderLayout.SOUTH);

			settings.add(pureMC, BorderLayout.NORTH);

			// Lib stuff - FAPI, CC, ZFG, ACFG, Terraform
			JPanel libs = new JPanel(new FlowLayout(FlowLayout.LEFT));
			libs.setBorder(new TitledBorder("Libraries"));

			JCheckBox fabricAPI = new JCheckBox("Fabric API");
			fabricAPI.setSelected(true);
			libs.add(fabricAPI);

			JCheckBox cardinalComponents = new JCheckBox("Cardinal Components");
			libs.add(cardinalComponents);

			JCheckBox zoesteriaConfig = new JCheckBox("Zoesteria Config");
			libs.add(zoesteriaConfig);

			JCheckBox autoConfig = new JCheckBox("AutoConfig");
			libs.add(autoConfig);

			JCheckBox terraform = new JCheckBox("Terraform");
			libs.add(terraform);

			settings.add(libs, BorderLayout.CENTER);
			master.add(settings, BorderLayout.CENTER);

			// bottom - button
			JButton create = new JButton();
			create.setText("Create Workspace");
			create.addActionListener(e -> {
				String wnm = modId.getText().trim(); // workspace name

				if (wnm != null && !wnm.isEmpty()) {
					File dir = new File(wnm);

					if (dir.isFile() || (dir.isDirectory() && dir.listFiles().length > 1)) {
						JOptionPane.showMessageDialog(frame, "A file/folder with the given mod id already exists in this directory.", "File/Folder already exists!", JOptionPane.ERROR_MESSAGE);
					} else {
						String group = mavenGroup.getText().trim();

						if (group.isEmpty()) {
							JOptionPane.showMessageDialog(frame, "Maven group is empty!", "Invalid maven group", JOptionPane.ERROR_MESSAGE);
						} else {
							dir.mkdirs();

							File gradleSettings = new File(dir, "settings.gradle");
							write(gradleSettings, readOnlineOrLocal("settings.gradle.txt"));

							File gradleBuild = new File(dir, "build.gradle");
							write(gradleBuild, readOnlineOrLocal("build.gradle.txt"));

							File gitignore = new File(dir, ".gitignore");
							write(gitignore, readOnlineOrLocal("gitignore.txt"));

							StringBuilder properties = new StringBuilder();
							properties.append("# Done to increase the memory available to gradle.\n")
							.append("org.gradle.jvmargs=-Xmx1G\n\n")
							.append("# Fabric Properties\n")
							.append("# check these on https://modmuss50.me/fabric.html")
							.append("\nminecraft_version=").append(minecraftVersion.getSelectedItem())
							.append("\nloader_version=").append(masterOptions.getStringValue("loader_latest"))
							.append("\nyarn_build=").append(yarnBuild.getText())
							.append("\n\n# Mod Properties")
							.append("\nmod_version=1.0.0")
							.append("\nmaven_group=").append(group)
							.append("\narchives_base_name=").append(wnm);

							File gradleProperties = new File(dir, "gradle.properties");
							write(gradleProperties, properties.toString());

							File wrapperDir = new File(dir, "gradle/wrapper");
							wrapperDir.mkdirs();

							File wrapperProperties = new File(wrapperDir, "gradle-wrapper.properties");
							write(wrapperProperties, readOnlineOrLocal("gradle-wrapper.properties.txt"));

							copyJar(new File(wrapperDir, "gradle-wrapper.jar"), "gradle-wrapper.jar");
						}
					}
					return;
				}

				JOptionPane.showMessageDialog(frame, "The mod id is empty!. Please try again.", "Invalid workspace name!", JOptionPane.ERROR_MESSAGE);
			});
			master.add(create, BorderLayout.SOUTH);

			frame.add(master);
			frame.setTitle("Fabric Setup");
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

			JDialog dialog = new JDialog(frame, "The Program has Crashed!", true);
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

	private static WritableConfig parseOnlineOrLocal(String name) {
		return new StringZFGParser<>(readOnlineOrLocal(name), new ImplZoesteriaDefaultDeserialiser(true)).asWritableConfig();
	}

	private static String readOnlineOrLocal(String name) {
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

		return result;
	}

	private static void copyJar(File destination, String name) {
		InputStream stream;

		try {
			URL url = new URL("https://raw.githubusercontent.com/valoeghese/Fabric-Setup/master/src/main/resources/" + name);
			stream = url.openStream();
		} catch (IOException e) {
			System.out.println("Likely no internet while retrieving online file for " + name + ", reverting to local copy.");
			stream = ResourceLoader.load(name);
		}

		try (FileOutputStream output = new FileOutputStream(destination)) {
			int nBytesRead;
			byte[] bufferBuffer = new byte[0x4000];

			while ((nBytesRead = stream.read(bufferBuffer, 0, bufferBuffer.length)) != -1) {
				output.write(bufferBuffer, 0, nBytesRead);
			}

			stream.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void write(File file, String string) {
		try (PrintWriter pw = new PrintWriter(file)) {
			pw.write(string);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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
