package mil.army.usace.hec.vortex.io;

import javafx.scene.transform.Translate;
import mil.army.usace.hec.vortex.GdalRegister;
import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexGrid;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class SnodasTarDataReader extends DataReader implements VirtualFileSystem{
    static {GdalRegister.getInstance();}
    SnodasTarDataReader(DataReaderBuilder builder) {super(builder);}

    @Override
    public List<VortexData> getDtos() {
        // Update tar with header files for SNODAS .dat files, and decompress all GZ files
        try {updateTar(this.path);} catch (IOException e) {e.printStackTrace();}
        // Get VirtualPath to Tar
        String vPath = getVirtualPath(this.path);
        Vector fileList = gdal.ReadDir(vPath);
        List<VortexData> dtos = new ArrayList<>();
        for(Object o : fileList) {
            String fileName = o.toString();
            if (fileName.endsWith(".dat")) {
                DataReader reader = DataReader.builder()
                        .path(vPath + fileName)
                        .variable(this.variableName)
                        .build();

                dtos.addAll(reader.getDtos());
            } // Extract grid from .dat files
        } // Loop through tar

        return dtos;
    } // getDtos()

    @Override
    public int getDtoCount() {
        return 0;
    } // getDtoCount()

    @Override
    public VortexData getDto(int idx) {
        return null;
    } // getDto()

    private void updateTar(String pathToFile) throws IOException {
        // Get DirectoryPath, TarFilePath, and TarFileName
        String tarFilePath = pathToFile;
        String directoryPath = Paths.get(tarFilePath).getParent().toString();
        String tarFileName = Paths.get(tarFilePath).getFileName().toString();

        // Create a File type object for the TarFile
        File tarFile = new File(tarFilePath, tarFileName);
        // Create a temp Tar File
        File tempTarFile = new File(tarFilePath, "tempTar.tar");
        tempTarFile.createNewFile();

        // Getting input stream from original TarFile, and output stream from new Temp Tar File
        TarArchiveInputStream originalStream = new TarArchiveInputStream(new FileInputStream(tarFile));
        TarArchiveOutputStream newStream = new TarArchiveOutputStream(new FileOutputStream(tempTarFile));

        // Creating entries from the original TarFile AND new header files, then add them into Temp Tar File
        ArchiveEntry nextEntry = null;
        List<String> headerNameArray = new ArrayList<>();
        // Reading entries from original TarFile, and save names for HeaderNameArray
        while((nextEntry = originalStream.getNextEntry()) != null) {
            // FIXME: Decompress GZ files
            String entryName = nextEntry.getName();
            if (entryName.contains(".dat")) {
                String headerName = entryName.substring(0, entryName.indexOf(".dat")) + ".hdr";
                headerNameArray.add(headerName);
            } // Save names for HeaderNameArray

            // Copy files in original TarFile to new TempTarFile
            newStream.putArchiveEntry(nextEntry);
            IOUtils.copy(originalStream, newStream);
            newStream.closeArchiveEntry();
        } // Copy original files from TarFile to TempTarFile

        // Creating header files and adding them
        for(String headerName : headerNameArray) {
            File headerFile = createHeader(directoryPath, headerName);
            TarArchiveEntry entry = new TarArchiveEntry(headerName);
            entry.setSize(headerFile.length());
            newStream.putArchiveEntry(entry);
            IOUtils.copy(new FileInputStream(headerFile), newStream);
        } // Loop through headerNameArray

        // Close streams
        newStream.closeArchiveEntry();
        newStream.finish();
        originalStream.close();
        newStream.close();

        // Delete original(old) TarFile, rename temp(new) TarFile
        tarFile.delete();
        tempTarFile.renameTo(tarFile);

        // FIXME: Delete the header files that are not in tar

    } // updateTar()

    private File createHeader(String directoryPath, String headerName) throws IOException {
        // Create empty header file
        File headerFile = new File(directoryPath, headerName);
        // Content of header
        List<String> enviHeader = Arrays.asList("ENVI", "samples = 6935", "lines = 3351", "bands = 1",
                "header offset = 0", "file type = ENVI Standard", "data type = 2", "interleave = bsq", "byte order = 1");
        // Write to HeaderFile
        Path header = Paths.get(headerName);
        Files.write(header, enviHeader, StandardCharsets.UTF_8);

        return headerFile;
    } // createHeader()
} // SnodasDataReader class