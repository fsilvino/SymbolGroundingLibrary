package sglib.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {

	private FileUtils() {
	}

	public static String readFileContents(String filePath) throws IOException {
		return new String(Files.readAllBytes(new File(filePath).toPath()));
	}

}
