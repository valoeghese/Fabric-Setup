package valoeghese.fabricsetup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// Taken and adapted from 2fc0f18 source.
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
}
