package com.oshosanya.jdownload.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class IOCopier {
    public static void joinFiles(File destination, List<File> sources, boolean deleteSource) throws IOException {
        try (OutputStream output = createAppendableStream(destination)) {

            for (File source : sources) {
                appendFile(output, source);
                if (deleteSource) {
                    FileUtils.deleteQuietly(source);
                }
            }
        } catch (Exception e) {
            System.out.printf("Unable to close file %s \n", e.getMessage());
        }
    }


    private static BufferedOutputStream createAppendableStream(File destination)
            throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(destination, true));
    }

    private static void appendFile(OutputStream output, File source) throws IOException {
        try (InputStream input = new BufferedInputStream(new FileInputStream(source))) {
            IOUtils.copy(input, output);
        } catch (Exception e) {
            System.out.printf("Unable to close file %s \n", e.getMessage());
        }
    }
}
