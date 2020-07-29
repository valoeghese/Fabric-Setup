package valoeghese.fabricsetup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

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
import javax.swing.border.TitledBorder;

import tk.valoeghese.zoesteriaconfig.api.container.Container;
import tk.valoeghese.zoesteriaconfig.api.container.WritableConfig;

public class Main {
	// Increment this and the ver in master.zfg when a change is made to the java program
	// (Not when merely changing resources, as they can be fetched from online)
	private static final int META_VER = 1;

	public static void main(String[] args) throws Throwable {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		try {
			WritableConfig masterOptions = ResourceManager.parseOnlineOrLocal("master.zfg");

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
				try {
					String workspaceModId = modId.getText().trim(); // workspace name / mod id

					if (workspaceModId != null && !workspaceModId.isEmpty()) {
						File dir = new File(workspaceModId);

						if (dir.isFile() || (dir.isDirectory() && dir.listFiles().length > 1)) {
							JOptionPane.showMessageDialog(frame, "A file/folder with the given mod id already exists in this directory.", "File/Folder already exists!", JOptionPane.ERROR_MESSAGE);
						} else {
							String group = mavenGroup.getText().trim();

							if (group.isEmpty()) {
								JOptionPane.showMessageDialog(frame, "Maven group is empty!", "Invalid maven group", JOptionPane.ERROR_MESSAGE);
							} else {
								dir.mkdirs();
								Container vsn = masterOptions.getContainer(((String) minecraftVersion.getSelectedItem()).replace('.', '-'));

								File gradleSettings = new File(dir, "settings.gradle");
								ResourceManager.write(gradleSettings, ResourceManager.readOnlineOrLocal("settings.gradle.txt"));

								// properties
								StringBuilder properties = new StringBuilder();
								properties.append("# Done to increase the memory available to gradle.")
								.append("\norg.gradle.jvmargs=-Xmx1G")
								.append("\n\n# Fabric Properties")
								.append("\n# check these on https://modmuss50.me/fabric.html")
								.append("\nminecraft_version=").append(minecraftVersion.getSelectedItem())
								.append("\nloader_version=").append(masterOptions.getStringValue("loader_latest"))
								.append("\nyarn_build=").append(yarnBuild.getText())
								.append("\n\n# Mod Properties")
								.append("\nmod_version=1.0.0")
								.append("\nmaven_group=").append(group)
								.append("\narchives_base_name=").append(workspaceModId)
								.append("\n\n# Other APIs");

								// Add libs to buildscript and properties
								StringBuilder libsScript = new StringBuilder();

								if (fabricAPI.isSelected()) {
									libsScript.append("\nmodImplementation \"net.fabricmc.fabric-api:fabric-api:${project.fabric_version}\"");
									properties.append("\nfabric_version=").append(vsn.getStringValue("fabric_api"));
								}

								if (cardinalComponents.isSelected()) {
									libsScript.append("\nmodImplementation \"com.github.OnyxStudios.Cardinal-Components-API:Cardinal-Components-API:${project.cardinal_version}\"");
									libsScript.append("\ninclude \"com.github.OnyxStudios.Cardinal-Components-API:Cardinal-Components-API:${project.cardinal_version}\"");
									properties.append("\ncardinal_version=").append(vsn.getStringValue("cardinal_components"));
								}

								if (zoesteriaConfig.isSelected()) {
									libsScript.append("\nimplementation \"tk.valoeghese:ZoesteriaConfig:${project.zoesteria_config_version}\"");
									libsScript.append("\ninclude \"tk.valoeghese:ZoesteriaConfig:${project.zoesteria_config_version}\"");
									properties.append("\nzoesteria_config_version=").append(masterOptions.getStringValue("zoesteria_config_latest"));
								}

								// Gradle stuff
								{
									File gradleBuild = new File(dir, "build.gradle");
									String buildScript = ResourceManager.readOnlineOrLocal("build.gradle.txt");
									ResourceManager.write(gradleBuild, buildScript);

									File gitignore = new File(dir, ".gitignore");
									ResourceManager.write(gitignore, ResourceManager.readOnlineOrLocal("gitignore.txt"));

									File gradleProperties = new File(dir, "gradle.properties");
									ResourceManager.write(gradleProperties, properties.toString());

									File wrapperDir = new File(dir, "gradle/wrapper");
									wrapperDir.mkdirs();

									File wrapperProperties = new File(wrapperDir, "gradle-wrapper.properties");
									ResourceManager.write(wrapperProperties, ResourceManager.readOnlineOrLocal("gradle/wrapper/gradle-wrapper.properties.txt"));

									ResourceManager.copyJar(new File(wrapperDir, "gradle-wrapper.jar"), "gradle/wrapper/gradle-wrapper.jar");
								}

								File run = new File(dir, "run");
								run.mkdir();

								String modName = toTitleCase(workspaceModId.replace('_', ' '));
								String mainClassName = modName.replaceAll(" ", "");
								Replacement replacement = new Replacement()
										.replaces("%MODID%", workspaceModId)
										.replaces("%MODNAME%", modName)
										.replaces("%MAINCLASS%", mainClassName)
										.replaces("%GROUP%", group);

								// Mod Resources
								{
									File srcmainresources = new File(dir, "src/main/resources");

									// Mod Json
									String modJsonContent = replacement.apply(ResourceManager.readOnlineOrLocal("fabric.mod.json"));
									File modJson = new File(srcmainresources, "fabric.mod.json");
									ResourceManager.write(modJson, modJsonContent);

									// Mixins Json
									String mixinJsonContent = replacement.apply(ResourceManager.readOnlineOrLocal("mixins.json"));
									File mixinsJson = new File(srcmainresources, workspaceModId + ".mixins.json");
									ResourceManager.write(mixinsJson, mixinJsonContent);
								}
							}
						}
						return;
					}

					JOptionPane.showMessageDialog(frame, "The mod id is empty!. Please input a mod id.", "Invalid workspace name!", JOptionPane.ERROR_MESSAGE);
				} catch (Throwable t) {
					byeBye(frame, t);
				}
			});
			master.add(create, BorderLayout.SOUTH);

			frame.add(master);
			frame.setTitle("Fabric Setup");
			frame.pack();
			frame.setVisible(true);

			int mVer = masterOptions.getIntegerValue("meta_version");

			if (mVer > META_VER) {
				JOptionPane.showMessageDialog(frame, "A new update is available. Please consider updating to the latest version of FabricSetup.");
			}
		} catch (Throwable t) {
			byeBye(frame, t);
		}
	}

	/*
	 * Thanks to some random guy on stack overflow for existing in 2009.
	 * NOTE: I changed toTitleCase to toUpperCase.
	 * https://stackoverflow.com/questions/1086123/string-conversion-to-title-case.
	 */
	public static String toTitleCase(String input) {
		StringBuilder result = new StringBuilder(input.length());
		boolean capitalFlag = true;

		for (char c : input.toCharArray()) {
			if (Character.isSpaceChar(c)) {
				capitalFlag = true;
			} else if (capitalFlag) {
				c = Character.toUpperCase(c);
				capitalFlag = false;
			}

			result.append(c);
		}

		return result.toString();
	}

	private static void byeBye(JFrame frame, Throwable t) {
		// I ripped this whole catch block from a swing ccg I'm making
		t.printStackTrace();

		StringBuilder sb = new StringBuilder();
		t.printStackTrace(new PrintStream(new StringOutputStream(sb)));

		JPanel container = new JPanel(new BorderLayout(0, 10));
		JTextArea error = ResourceManager.makeWrapping(new JLabel("The following error occurred."));

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
