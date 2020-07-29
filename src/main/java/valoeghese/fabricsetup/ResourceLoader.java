package valoeghese.fabricsetup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import tk.valoeghese.zoesteriaconfig.api.container.WritableConfig;
import tk.valoeghese.zoesteriaconfig.impl.parser.ImplZoesteriaDefaultDeserialiser;

// Taken and adapted from 2fc0f18 source, adding extra functionality as well.
public final class ResourceLoader {
	public static URL loadURL(String location) {
		return ResourceLoader.class.getClassLoader().getResource(location);
	}

	public static InputStream load(String location) {
		return ResourceLoader.class.getClassLoader().getResourceAsStream(location);
	}

	public static String loadAsString(String location) throws IOException {
		return readString(() -> load(location));
	}

	public static String readString(FallableIOSupplier<InputStream> isSupplier) throws IOException {
		InputStream is = isSupplier.get();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nBytesRead;
		byte[] bufferBuffer = new byte[0x4000];

		while ((nBytesRead = is.read(bufferBuffer, 0, bufferBuffer.length)) != -1) {
			buffer.write(bufferBuffer, 0, nBytesRead);
		}

		is.close();
		return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
	}

	public static WritableConfig parseOnlineOrLocal(String name) {
		return new StringZFGParser<>(readOnlineOrLocal(name), new ImplZoesteriaDefaultDeserialiser(true)).asWritableConfig();
	}

	public static String readOnlineOrLocal(String name) {
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

	public static void copyJar(File destination, String name) {
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

	public static void write(File file, String string) {
		try (PrintWriter pw = new PrintWriter(file)) {
			pw.write(string);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// wow thanks stackoverflow
	public static JTextArea makeWrapping(JLabel label) {
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
}
