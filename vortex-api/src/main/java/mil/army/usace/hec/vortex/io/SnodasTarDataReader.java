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
import org.apache.commons.io.FileUtils;

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
        String folderPath = Paths.get(this.path).getParent().toString() + File.separator + "unzipFolder" + File.separator;
        // Use Gdal to read in data
        Vector fileList = gdal.ReadDir(folderPath);
        List<VortexData> dtos = new ArrayList<>();
        for(Object o : fileList) {
            String fileName = o.toString();
            if (fileName.endsWith(".dat")) {
                DataReader reader = DataReader.builder()
                        .path(folderPath + fileName)
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

        // Creating an input stream for the original tar file
        File originalTarFile = new File(directoryPath, tarFileName);
        TarArchiveInputStream originalStream = new TarArchiveInputStream(new FileInputStream(originalTarFile));

        // Extract files from tar into untarFolder
        File untarFolder = unTarFile(directoryPath, originalStream);
        // Close originalStream
        originalStream.close();
        // Decompress GZ files inside untarFolder to gzFolder
        File gzFolder = decompressFolder(directoryPath, untarFolder);
        // Create header files
        for(File fileEntry : gzFolder.listFiles()) {
            String fileName = fileEntry.getName();
            if(fileName.endsWith(".dat")) {
                String name = fileName.substring(0, fileName.lastIndexOf(".dat")) + ".hdr";
                String gzFolderPath = directoryPath + File.separator + "unzipFolder";
                createHeader(gzFolderPath, name);
            }
        } // Loop through unzipped Folder
        // Compress folder into a Tar file
        File tempTarFile = tarFolder(directoryPath, gzFolder);

        // Clean up: deleting untarFolder, unzipFolder, original tar, and rename tempTar to original
        FileUtils.deleteDirectory(untarFolder);
//        FileUtils.deleteDirectory(gzFolder);
        FileUtils.deleteQuietly(originalTarFile);
        tempTarFile.renameTo(originalTarFile);
    } // updateTar()


    private File tarFolder(String directoryPath, File inputFolder) throws IOException {
        // Create a temp Tar File
        File tempTarFile = new File(directoryPath, "tempTar.tar");
        tempTarFile.createNewFile();
        TarArchiveOutputStream newStream = new TarArchiveOutputStream(new FileOutputStream(tempTarFile));

        for(File currentFile : inputFolder.listFiles()) {
            ArchiveEntry entry = newStream.createArchiveEntry(currentFile, currentFile.getName());
            newStream.putArchiveEntry(entry);
            InputStream folderStream = Files.newInputStream(currentFile.toPath());
            IOUtils.copy(folderStream, newStream);
            folderStream.close();
            newStream.closeArchiveEntry();
        }
        newStream.close();

        return tempTarFile;
    } // tarFolder

    private File unTarFile(String directoryPath, TarArchiveInputStream iStream) throws IOException {
        String fileSeparator = File.separator;
        ArchiveEntry nextEntry = null;
        // Creating a folder to untar into
        String folderPath = directoryPath + fileSeparator + "untarFolder";
        File destinationFolder = new File(folderPath);
        if(!destinationFolder.exists())
            destinationFolder.mkdirs();

        // Untar into folder
        while((nextEntry = iStream.getNextEntry()) != null) {
            File outputFile = new File(destinationFolder + fileSeparator + nextEntry.getName());
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            IOUtils.copy(iStream, outputStream);
            outputStream.close();
        } // Untar into folder

        return destinationFolder;
    } // unTarFile

    private File decompressFolder(String directoryPath, File inputFolder) throws IOException {
        String fileSeparator = File.separator;
        // Creating a folder to unzip into
        String gzFolderPath = directoryPath + fileSeparator + "unzipFolder";
        File gzDestinationFolder = new File(gzFolderPath);
        if(!gzDestinationFolder.exists())
            gzDestinationFolder.mkdirs();

        // Loop through inputFolder
        if(inputFolder.isDirectory()) {
            for (File fileEntry : Objects.requireNonNull(inputFolder.listFiles())) {
                String name = fileEntry.getName().substring(0, fileEntry.getName().lastIndexOf(".gz"));
                File decompressedOutputFile = new File(gzDestinationFolder + fileSeparator + name);
                GzipCompressorInputStream gzStream = new GzipCompressorInputStream(new FileInputStream(fileEntry));
                FileOutputStream decompressedOutputStream = new FileOutputStream(decompressedOutputFile);
                IOUtils.copy(gzStream, decompressedOutputStream);
                gzStream.close();
                decompressedOutputStream.close();
            }
        } // unzipFolder contains decompressed files

        return gzDestinationFolder;
    } // decompressFolder

    private File createHeader(String directoryPath, String headerName) throws IOException {
        // Create empty header file
        File headerFile = new File(directoryPath, headerName);
        // Content of header
        List<String> enviHeader = Arrays.asList("ENVI", "samples = 6935", "lines = 3351", "bands = 1",
                "header offset = 0", "file type = ENVI Standard", "data type = 2", "interleave = bsq", "byte order = 1");
        // Write to HeaderFile
        Path header = Paths.get(directoryPath + File.separator + headerName);
        Files.write(header, enviHeader, StandardCharsets.UTF_8);

        return headerFile;
    } // createHeader()
} // SnodasDataReader class