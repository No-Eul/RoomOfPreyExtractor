package dev.noeul.roomofprey.extractor;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RoomOfPreyExtractor {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Argument usage: <input_file> [<output_dir>] [<filelist_hex_hash>]");
			return;
		}

		File file = new File(args[0]);
		if (!file.exists()) {
			System.out.printf("[Error] File '%s' does not exist", file.getAbsolutePath());
			return;
		} else if (!file.isFile()) {
			System.out.printf("[Error] File '%s' is not valid file", file.getAbsolutePath());
			return;
		} else if (!file.canRead()) {
			System.out.printf("[Error] Cannot read file '%s'", file.getAbsolutePath());
			return;
		}

		File outputDir;
		if (args.length > 1) {
			outputDir = new File(args[1]).getAbsoluteFile();
			if (!(outputDir.exists() && outputDir.isDirectory() || outputDir.mkdirs())) {
				outputDir = new File(".");
				System.out.println("[Warn] Could not change output directory; Will be saved extracted file to current directory");
			}
		} else {
			outputDir = new File(".");
			System.out.println("[Info] You've not specified the output directory; Will be extracted to the current directory");
		}

		int fileListHash = 0xBC909D54;
		if (args.length > 2) {
			try {
				fileListHash = Integer.parseUnsignedInt(args[2], 16);
				System.out.printf("[Info] File list hash value has specified: $x%n", fileListHash);
			} catch (NumberFormatException e) {
				System.err.printf("[Error] '%s' is not valid 32-bit hex number%n", args[1]);
				return;
			}
		}

		try (RandomAccessFile stream = new RandomAccessFile(file, "r")) {
			Map<Integer, String> map = new HashMap<>();

			try { // Filename list handling
				while (readIntLE(stream) != fileListHash)
					stream.skipBytes(readIntLE(stream) + 4);
				stream.skipBytes(4);
				int fileCount = readIntLE(stream);
				System.out.printf("[Info] Named file count: %d, Gathering filename...%n", fileCount);

				for (int i = 0; i < fileCount; i++) {
					String fileName = readString(stream, StandardCharsets.UTF_8);
					int hash = hash(fileName);
					System.out.printf("FileName: %s, Hash: %08x%n", map.put(hash, fileName), hash);
				}
			} catch (EOFException e) {
				System.out.println("[Warn] Could not find filename list data");
			}

			// Extracting
			stream.seek(0);
			System.out.println("================================================================================================");
			System.out.printf("Extracting... (%8s %8s %8s) => Output%n", "Hash", "Offset", "Length");
			while (stream.getFilePointer() < stream.length()) {
				int hash = readIntLE(stream);
				int len = readIntLE(stream);

				File output = new File(outputDir, map.computeIfAbsent(hash, key -> String.format("%s_%08x", file.getName(), key)));
				File dir = output.getAbsoluteFile().getParentFile();
				if (!(dir.exists() && dir.isDirectory() || dir.mkdirs())) {
					System.out.printf("[Warn] Cannot create the directory: %s%n", dir);
					System.out.println("[Warn] Skipped and item will be not extracted");
					stream.skipBytes(len);
					break;
				}

				System.out.printf("Extracting... (%08x %08x %08x) => %s%n", hash, stream.getFilePointer(), len, output);
				try (FileOutputStream outputStream = new FileOutputStream(output)) {
					byte[] buf = new byte[4096];
					while (len > 0) {
						int reads = stream.read(buf, 0, Math.min(buf.length, len));
						outputStream.write(buf, 0, reads);
						len -= reads;
					}
				} catch (IOException e) {
					System.err.println("[Error] Could not extract item:");
					e.printStackTrace();
				}
			}
			System.out.println("Done!");
		} catch (EOFException e) {
			System.out.println("[Warn] Done: EOF while reading file " + file.getAbsolutePath());
		} catch (IOException e) {
			System.err.println("[Error] Could not extract the whole data:");
			e.printStackTrace();
		}
	}

	public static int hash(String str) {
		int result = 5381;
		if (str == null || str.isEmpty())
			return result;
		for (byte b : str.getBytes(StandardCharsets.UTF_8))
			result += result * 32 + (b & 0xFF);
		return result;
	}

	public static int readIntLE(RandomAccessFile stream) throws IOException {
		int result = 0;
		for (int i = 0; i < Integer.BYTES; i++) {
			int read = stream.read();
			if (read >= 0) result |= read << Byte.SIZE * i;
			else throw new EOFException();
		}
		return result;
	}

	public static String readString(RandomAccessFile stream, Charset charset) throws IOException {
		long off = stream.getFilePointer();
		while (stream.read() > 0) ;
		byte[] bytes = new byte[(int) (stream.getFilePointer() - off - 1)];
		stream.seek(off);
		stream.read(bytes, 0, bytes.length);
		stream.skipBytes(1);
		return new String(bytes, charset);
	}
}
