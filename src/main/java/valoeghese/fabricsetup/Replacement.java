package valoeghese.fabricsetup;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class Replacement implements UnaryOperator<String> {
	private final Map<String, String> replacements = new HashMap<>();

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
