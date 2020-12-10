package valoeghese.fabricsetup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import tk.valoeghese.zoesteriaconfig.api.ZoesteriaConfig;
import tk.valoeghese.zoesteriaconfig.api.container.Container;
import tk.valoeghese.zoesteriaconfig.api.container.WritableConfig;
import tk.valoeghese.zoesteriaconfig.api.deserialiser.Comment;

public class Main {
	// Increment this and the ver in master.zfg when a change is made to the java program
	// (Not when merely changing resources, as they can be fetched from online)
	private static final int META_VER = 3;

	public static final int DEFAULT_WIDTH = 300;

	public static void main(String[] args) throws Throwable {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		try {
			final WritableConfig masterOptions = ResourceManager.parseOnlineOrLocal("master.zfg");
			//			System.out.println(masterOptions.getIntegerValue("meta_version"));
			Library.addMCLibs(masterOptions.getList("mclibs"), masterOptions.getContainer("mclibsData"));

			JPanel master = new JPanel(new BorderLayout());
			master.setPreferredSize(new Dimension(DEFAULT_WIDTH, 375));

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
			final JComboBox<String> minecraftVersion = new JComboBox<>(vs);
			minecraftVersion.setBorder(new TitledBorder("Minecraft Version"));

			// action listener to update available libraries
			minecraftVersion.addActionListener(e -> {
				updateAvailableLibs(masterOptions, (String) minecraftVersion.getSelectedItem());
			});

			pureMC.add(minecraftVersion, BorderLayout.NORTH);

			JTextField yarnBuild = new JTextField();
			yarnBuild.setBorder(new TitledBorder("Yarn Build"));
			yarnBuild.setText(masterOptions.getStringValue(vs[0].replace('.', '-') + ".yarn_latest"));
			pureMC.add(yarnBuild, BorderLayout.SOUTH);

			settings.add(pureMC, BorderLayout.NORTH);

			// Lib stuff - FAPI, CC, ZFG, ACFG, Terraform
			JPanel libs = new JPanel(new BorderLayout());
			libs.setBorder(new TitledBorder("Libraries"));

			JPanel lists = new JPanel(new BorderLayout());

			JList<String> options = Library.options();
			options.setBorder(new TitledBorder("Unselected"));
			options.setPreferredSize(new Dimension(DEFAULT_WIDTH / 2 - 15, 150));
			lists.add(new JScrollPane(options), BorderLayout.WEST);

			JList<String> selected = Library.selected();
			selected.setBorder(new TitledBorder("Selected"));
			selected.setPreferredSize(new Dimension(DEFAULT_WIDTH / 2 - 15, 150));
			lists.add(new JScrollPane(selected), BorderLayout.EAST);

			libs.add(lists, BorderLayout.CENTER);

			// buttons
			JPanel buttons = new JPanel(new BorderLayout());
			buttons.setPreferredSize(new Dimension(DEFAULT_WIDTH, 20));

			JButton sel = new JButton("Select");
			sel.addActionListener(event -> {
				String val = options.getSelectedValue();

				if (val != null) {
					Library.select(val);
				}
			});
			buttons.add(sel, BorderLayout.WEST);

			JButton desel = new JButton("Deselect");
			desel.addActionListener(event -> {
				String val = selected.getSelectedValue();

				if (val != null) {
					Library.deselect(val);
				}
			});
			buttons.add(desel, BorderLayout.EAST);

			libs.add(buttons, BorderLayout.SOUTH);

			settings.add(libs, BorderLayout.CENTER);
			master.add(settings, BorderLayout.CENTER);

			// update available sh1t
			updateAvailableLibs(masterOptions, (String) minecraftVersion.getSelectedItem());

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

								Library.getSelected().forEach(lib -> {
									String version = lib.mcDependent ? vsn.getStringValue(lib.manifestKey) : masterOptions.getStringValue(lib.manifestKey);
									properties.append("\n\t").append(lib.propertiesKey).append('=').append(version);
									libsScript.append("\n\t").append(lib.mcDependent ? "modImplementation" : "implementation").append(" \"").append(lib.mavenKey).append(":${project.").append(lib.propertiesKey).append("}\"");

									if (!lib.name.equals("Fabric API")) {
										libsScript.append("\n\tinclude \"").append(lib.mavenKey).append(":${project.").append(lib.propertiesKey).append("}\"");
									}
								});

								// Gradle stuff
								{
									File gradleBat = new File(dir, "gradlew.bat");
									ResourceManager.write(gradleBat, ResourceManager.readOnlineOrLocal("gradlew.bat.txt"));

									File gradlew = new File(dir, "gradlew");
									ResourceManager.write(gradlew, ResourceManager.readOnlineOrLocal("gradlew.txt"));

									File gradleBuild = new File(dir, "build.gradle");
									String buildScript = ResourceManager.readOnlineOrLocal("build.gradle.txt");
									buildScript = buildScript.replace("INJECTHERE", libsScript.toString());
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

								// mod manifest
								createSelected((String) minecraftVersion.getSelectedItem(),
										yarnBuild.getText(),
										group,
										workspaceModId,
										Library.getSelectedNames()).writeToFile(new File(dir, "fsetup_manifest.zfg"));

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
									srcmainresources.mkdirs();

									// Mod Json
									String modJsonContent = replacement.apply(ResourceManager.readOnlineOrLocal("fabric.mod.json"));
									File modJson = new File(srcmainresources, "fabric.mod.json");
									ResourceManager.write(modJson, modJsonContent);

									// Mixins Json
									String mixinJsonContent = replacement.apply(ResourceManager.readOnlineOrLocal("mixins.json"));
									File mixinsJson = new File(srcmainresources, workspaceModId + ".mixins.json");
									ResourceManager.write(mixinsJson, mixinJsonContent);
								}

								// Mod Sources
								{
									File srcmainjava = new File(dir, "src/main/java");
									srcmainjava.mkdirs();

									// Main Class
									String mainClassContent = replacement.apply(ResourceManager.readOnlineOrLocal("mainclass.txt"));
									File mainPackage = new File(srcmainjava, group.replace('.', '/') + "/" + workspaceModId);
									mainPackage.mkdirs();
									File mainClass = new File(mainPackage, mainClassName + ".java");
									ResourceManager.write(mainClass, mainClassContent);
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

			System.out.println(masterOptions.asMap());
			int latestVer = masterOptions.getIntegerValue("meta_version");
			String title = "Fabric Setup";

			if (META_VER > latestVer) {
				title += " [Dev]";
			}

			frame.add(master);
			frame.setTitle(title);
			frame.pack();
			frame.setVisible(true);

			if (latestVer > META_VER) {
				JOptionPane.showMessageDialog(frame, "A new update is available. Please consider updating to the latest version of FabricSetup.");
			}
		} catch (Throwable t) {
			byeBye(frame, t);
		}
	}

	private static void updateAvailableLibs(Container masterOptions, String version) {
		version = version.replace('.', '-');
		Container data = masterOptions.getContainer(version);
		Library.update(name -> data.containsKey(Library.getManifestKey(name)));
	}

	/*
	 * Thanks to some random guy on stack overflow for existing in 2009.
	 * NOTE: I changed toTitleCase call to to toUpperCase.
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

	private static WritableConfig createSelected(String mcVer, String yarnVer, String group, String modid, List<Object> libs) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put(".comment", new Comment("Will be used in the future for updating workspaces to new versions."));
		WritableConfig result = ZoesteriaConfig.createWritableConfig(data);
		//		result.addComment("Will be used in the future for updating workspaces to new versions.");
		result.putStringValue("minecraft", mcVer);
		result.putStringValue("yarn", yarnVer);
		result.putStringValue("group", group);
		result.putStringValue("modid", modid);
		result.putList("libs", libs);

		return result;
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
