package valoeghese.fabricsetup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.DefaultListModel;
import javax.swing.JList;

public class Library {
	private Library (boolean mcDependent, String mavenKey, String propertiesKey, String manifestKey, String name) {
		REVERSE_MAP.put(name, this);
		LIBS_OPTIONS.addElement(name);

		this.mcDependent = mcDependent;
		this.mavenKey = mavenKey;
		this.propertiesKey = propertiesKey;
		this.manifestKey = manifestKey;
		this.name = name;
	}

	public final boolean mcDependent;
	public final String mavenKey;
	public final String propertiesKey;
	public final String manifestKey;
	public final String name;

	public static JList<String> options() {
		return new JList<>(LIBS_OPTIONS);
	}

	public static JList<String> selected() {
		return new JList<>(LIBS_SELECTED);
	}

	public static void select(String name) {
		LIBS_OPTIONS.removeElement(name);
		LIBS_SELECTED.addElement(name);
	}

	public static void deselect(String name) {
		LIBS_SELECTED.removeElement(name);
		LIBS_OPTIONS.addElement(name);
	}

	public static Stream<Library> getSelected() {
		return Stream.of(LIBS_SELECTED.toArray())
				.map(REVERSE_MAP::get)
				.filter(l -> l != null);
	}

	private static final Map<Object, Library> REVERSE_MAP = new LinkedHashMap<>();
	private static final DefaultListModel<String> LIBS_OPTIONS = new DefaultListModel<>();
	private static final DefaultListModel<String> LIBS_SELECTED = new DefaultListModel<>();

	public static final Library FABRIC = new Library(true, "net.fabricmc.fabric-api:fabric-api", "fabric_version", "fabric_api", "Fabric API");
	public static final Library CARDINAL = new Library(true, "com.github.OnyxStudios.Cardinal-Components-API:Cardinal-Components-API", "cardinal_version", "cardinal_components", "Cardinal Components");
	public static final Library ZOESTERIA_CONFIG = new Library(false, "tk.valoeghese:ZoesteriaConfig", "zoesteria_config_version", "zoesteria_config_latest", "ZoesteriaConfig");
	public static final Library AUTOCONFIG = new Library(true, "me.sargunvohra.mcmods:autoconfig1u", "autoconfig_version", "auto_config", "Auto Config");
	public static final Library TERRAFORM = new Library(true, "com.terraformersmc:terraform", "terraform_version", "terraform", "Terraform");

	static {
		select("Fabric API");
	}
}
