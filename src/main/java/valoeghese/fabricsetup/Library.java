package valoeghese.fabricsetup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import tk.valoeghese.zoesteriaconfig.api.container.Container;

public class Library {
	private Library (boolean mcDependent, String mavenKey, String propertiesKey, String manifestKey, String name) {
		REVERSE_MAP.put(name, this);

		if (name.equals("Fabric API")) {
			LIBS_SELECTED.addElement(name);
		} else {
			LIBS_OPTIONS.addElement(name);
		}

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

	public static void addMCLibs(List<Object> keys, Container data) {
		for (Object o : keys) {
			String key = (String) o;
			List<Object> libData = data.getList(key);
			MCLIBS.add(new Library(true, (String) libData.get(0), (String) libData.get(1), key, (String) libData.get(2)));
		}
	}

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

	public static List<Object> getSelectedNames() {
		return Arrays.asList(LIBS_SELECTED.toArray());
	}

	public static Stream<Library> getSelected() {
		return Stream.of(LIBS_SELECTED.toArray())
				.map(REVERSE_MAP::get)
				.filter(l -> l != null);
	}

	private static final Map<Object, Library> REVERSE_MAP = new LinkedHashMap<>();
	private static final DefaultListModel<String> LIBS_OPTIONS = new DefaultListModel<>();
	private static final DefaultListModel<String> LIBS_SELECTED = new DefaultListModel<>();
	private static final List<Library> MCLIBS = new ArrayList<>();

	public static final Library ZOESTERIA_CONFIG = new Library(false, "tk.valoeghese:ZoesteriaConfig", "zoesteria_config_version", "zoesteria_config_latest", "ZoesteriaConfig");
}
