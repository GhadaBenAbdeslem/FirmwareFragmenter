/*
 * Copyright (C) 2016 Digi International Inc., All Rights Reserved
 *
 * This software contains proprietary and confidential information of Digi.
 * International Inc. By accepting transfer of this copy, Recipient agrees
 * to retain this software in confidence, to prevent disclosure to others,
 * and to make no use of this software other than that for which it was
 * delivered. This is an unpublished copyrighted work of Digi International
 * Inc. Except as permitted by federal law, 17 USC 117, copying is strictly
 * prohibited.
 *
 * Restricted Rights Legend
 *
 * Use, duplication, or disclosure by the Government is subject to restrictions
 * set forth in sub-paragraph (c)(1)(ii) of The Rights in Technical Data and
 * Computer Software clause at DFARS 252.227-7031 or subparagraphs (c)(1) and
 * (2) of the Commercial Computer Software - Restricted Rights at 48 CFR
 * 52.227-19, as applicable.
 *
 * Digi International Inc. 11001 Bren Road East, Minnetonka, MN 55343
 */

package com.digi.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class used to fragment the firmware update package.
 */
public class FirmwareFragmenter {

	// Constants.
	private static final int DEFAULT_FRAGMENT_SIZE = 40; // 40 MB
	private static final String DEFAULT_SRC_DIR = "/storage/emulated/legacy";
	private static final String OUT_DIR = "out";

	private static final String ARG_SIZE = "-s";
	private static final String ARG_DIR = "-d";

	private static final String MANIFEST_NAME = "manifest.txt";
	private static final String PROPERTY_FRAGMENTS = "fragments";
	private static final String PROPERTY_NAME = "name";
	private static final String PROPERTY_CHECKSUM = "checksum";
	private static final String PROPERTY_SIZE = "size";
	private static final String PROPERTY_SRC_DIR = "src_dir";

	private static final String ERROR_INVALID_SIZE = "Invalid max fragment size, using " +
			DEFAULT_FRAGMENT_SIZE + " MB";

	// Variables.
	private String name;
	private String srcDir;
	private long maxSize;
	private int fragments;
	private File source;
	private File outputDir;
	private CRC32 crc;
	private FileInputStream fileStream;

	/**
	 * Class constructor.
	 *
	 * @param fwFile Firmware update package file.
	 * @param srcDir Directory in the Android storage where the fragments will
	 *               be located.
	 * @param maxSize Fragments max size in bytes.
	 *
	 * @throws FileNotFoundException If {@code fwFile} cannot be found or is
	 *                               not a valid file.
	 */
	public FirmwareFragmenter(String fwFile, String srcDir, long maxSize)
			throws FileNotFoundException {
		this.source = new File(fwFile);
		this.name = source.getName().substring(0, source.getName().lastIndexOf("."));
		this.srcDir = srcDir;
		this.maxSize = maxSize;

		if (!source.exists() || !source.isFile() || !source.canRead()) {
			throw new FileNotFoundException(fwFile + " is not a valid file");
		}

		// Initialize checksum and InputStream.
		crc = new CRC32();
		fileStream = new FileInputStream(source);

		// Create the output directory.
		outputDir = new File(OUT_DIR);
		if (!outputDir.isDirectory())
			outputDir.mkdirs();

		// Calculate number of fragments.
		fragments = (int)(source.length() / maxSize);
		if (source.length() % maxSize != 0)
			fragments++;

		System.out.println("Fragmenting " + fwFile + " into " + fragments + " fragments...");
	}

	/**
	 * Creates the fragments from the original update package file.
	 */
	public void fragmentUpdatePackage() {
		try {
			// Write each individual fragment into a separate ZIP file.
			for (int i = 0; i < fragments; i++) {
				System.out.print("Processing fragment " + i + "... ");
				writeFragment(i);
				System.out.println("Done");
			}
			System.out.print("Creating manifest...");
			createManifest();
			System.out.println("Done");
			System.out.println("Update package is now fragmented");
		} catch (IOException e) {
			System.err.println("Error while writing fragments");
			e.printStackTrace();
		} finally {
			try {
				fileStream.close();
			} catch (IOException e) {}
		}
	}

	/**
	 * Writes the specific fragment.
	 *
	 * @param fragment Number of fragment.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	private void writeFragment(int fragment) throws IOException {
		ZipEntry ze = new ZipEntry(source.getName());
		ZipOutputStream zos = null;

		try {
			zos = new ZipOutputStream(new FileOutputStream(outputDir.getName() +
					"/" + name + fragment + ".zip"));
			zos.putNextEntry(ze);

			byte[] buffer = new byte[64 * 1024];
			long total = 0;
			int bytesRead;
			while (total < maxSize && (bytesRead = fileStream.read(buffer)) != -1) {
				zos.write(buffer, 0, bytesRead);
				crc.update(buffer, 0, bytesRead);
				total += bytesRead;
			}
		} finally {
			if (zos != null) {
				try {
					zos.closeEntry();
				} catch (IOException e) {}
				try {
					zos.close();
				} catch (IOException e) {}
			}
		}
	}

	/**
	 * Creates the manifest file.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	private void createManifest() throws IOException {
		Properties manifest = new Properties();

		// Create manifest.
		manifest.setProperty(PROPERTY_SIZE, new Long(source.length()).toString());
		manifest.setProperty(PROPERTY_FRAGMENTS, new Integer(fragments).toString());
		manifest.setProperty(PROPERTY_NAME, name);
		manifest.setProperty(PROPERTY_CHECKSUM, new Long(crc.getValue()).toString());
		manifest.setProperty(PROPERTY_SRC_DIR, srcDir);

		File manifestFile = new File(outputDir, MANIFEST_NAME);
		Writer w = new PrintWriter(new FileOutputStream(manifestFile));

		// Store the manifest in manifest.txt.
		manifest.store(w, "Manifest file");

		w.close();
	}

	/**
	 * Application entry point.
	 *
	 * @param args Application arguments.
	 */
	public static void main(String[] args) {
		String fwFile = null, srcDir = DEFAULT_SRC_DIR;
		long maxSize = DEFAULT_FRAGMENT_SIZE;

		// Parse arguments.
		if (args.length == 1) {
			fwFile = args[0];
		} else if (args.length > 2) {
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
				case ARG_SIZE:
					try {
						maxSize = Long.parseLong(args[i + 1]);
						if (maxSize < 1) {
							maxSize = DEFAULT_FRAGMENT_SIZE;
							System.err.println(ERROR_INVALID_SIZE);
						}
					} catch (NumberFormatException e) {
						System.err.println(ERROR_INVALID_SIZE);
					}
					i++;
					break;
				case ARG_DIR:
					srcDir = args[i + 1];
					i++;
					break;
				default:
					fwFile = args[i];
					break;
				}
			}
		} else {
			System.out.format("Usage: java -jar %s.jar [%s <max_fragment_size_mb>] " +
					"[%s <source_dir>] <update_package_path>\n",
					FirmwareFragmenter.class.getSimpleName(), ARG_SIZE, ARG_DIR);
			return;
		}

		try {
			new FirmwareFragmenter(fwFile, srcDir, maxSize * 1000 * 1000).fragmentUpdatePackage();
		} catch (FileNotFoundException e) {
			System.err.println("Error during fragmentation process");
			e.printStackTrace();
		}
	}
}