package valoeghese.fabricsetup;

import java.util.ArrayList;
import java.util.List;

import tk.valoeghese.zoesteriaconfig.api.container.WritableConfig;
import tk.valoeghese.zoesteriaconfig.api.deserialiser.Comment;
import tk.valoeghese.zoesteriaconfig.api.deserialiser.ZFGContainerDeserialiser;
import tk.valoeghese.zoesteriaconfig.api.deserialiser.ZFGExtendedDeserialiser;
import tk.valoeghese.zoesteriaconfig.impl.parser.ImplZoesteriaConfigParser;

public class StringZFGParser<E extends ZFGExtendedDeserialiser<T>, T> {
	private int index, bufferSize;
	protected final E deserialiser;

	public StringZFGParser(String s, E deserialiser) {
		char[] charBuffer = s.toCharArray();
		this.index = -1;
		this.bufferSize = charBuffer.length;
		this.deserialiser = deserialiser;
		this.parseContainer(this.deserialiser, charBuffer);

	}

	private ZFGContainerDeserialiser parseContainer(ZFGContainerDeserialiser container, char[] data) {
		byte mode = 0; // 0 = var names, 1 = var values
		StringBuilder buffer = new StringBuilder();

		while (this.index + 1 < this.bufferSize) {
			++this.index;
			char c = data[this.index];

			if (c == '}') {
				break;
			} else if (c == '#') { // comment
				container.readComment(this.parseComment(data));
			} else if (mode == 1) {
				if (!Character.isWhitespace(c)) {
					if (c == '{') { // new container
						this.parseContainer(container.createSubContainer(buffer.toString()), data);
					} else if (c == '[') { // new list
						List<Object> list = this.parseList(data);
						container.readList(buffer.toString(), list);
					} else if (c == ';') { // new empty data object
						container.readData(buffer.toString(), "");
					} else { // new data object
						container.readData(buffer.toString(), this.parseData(data));
					}

					buffer = new StringBuilder();
					mode = 0;
				}
			} else if (c == '=') {
				mode = 1;
			} else if (!Character.isWhitespace(c)) {
				buffer.append(c); // append character to string buffer
			}
		}

		return container;
	}

	private List<Object> parseList(char[] data) {
		List<Object> result = new ArrayList<>();

		while (this.index + 1 < this.bufferSize) {
			++this.index;
			char c = data[index];

			if (c == ']') {
				break;
			} else if (c == '#') {
				// TODO in ZFG 1.3.5 make comment stripping apply to within lists
				//				result.add(this.parseComment(data));
			} else if (!Character.isWhitespace(c)) {
				if (c == '{') { // new container
					result.add(this.parseContainer(this.deserialiser.newContainerDeserialiser(), data));
				} else if (c == '[') { // new list
					result.add(this.parseList(data));
				} else if (c == ';') { // new empty data object
					result.add("");
				} else { // new data object
					result.add(this.parseData(data));
				}
			}
		}

		return result;
	}

	private String parseData(char[] data) {
		StringBuilder buffer = new StringBuilder().append(data[this.index]); // initial character is already at the index

		while (this.index + 1 < this.bufferSize) {
			++this.index;
			char c = data[this.index];

			if (c == ';') {
				break;
			} else if ((!Character.isWhitespace(c)) || c == ' ') {
				// the only form of whitespace in data values allowed is spaces
				// tabs, carriage return, and line feed are considered merely formatting
				buffer.append(c);
			}
		}

		return buffer.toString().trim(); // remove trailing whitespace
	}

	private Comment parseComment(char[] data) {
		StringBuilder buffer = new StringBuilder();

		while (this.index + 1 < this.bufferSize) {
			++this.index;
			char c = data[this.index];

			if (c == '\n') { // break comment on new line
				break;
			}

			buffer.append(c);
		}

		return new Comment(buffer.toString());
	}

	public E getDeserialiser() {
		return this.deserialiser;
	}

	@Override
	public String toString() {
		return "parserOf(" + this.deserialiser.toString() + ")";
	}

	public WritableConfig asWritableConfig() {
		return ImplZoesteriaConfigParser.createAccess(this.deserialiser.asMap());
	}
}