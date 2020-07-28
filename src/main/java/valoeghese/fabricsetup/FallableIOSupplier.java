package valoeghese.fabricsetup;

import java.io.IOException;

public interface FallableIOSupplier<T> {
	T get() throws IOException;
}
