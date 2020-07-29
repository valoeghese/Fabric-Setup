package valoeghese.fabricsetup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class Replacement implements UnaryOperator<String> {
	private final Map<String, String> replacements = new LinkedHashMap<>();

	public Replacement replaces(String regex, String with) {
		this.replacements.put(regex, with);
		return this;
	}

	@Override
	public String apply(String t) {
		for (Map.Entry<String, String> entry : this.replacements.entrySet()) {
			t = t.replaceAll(entry.getKey(), entry.getValue());
		}

		return t;
	}
}
