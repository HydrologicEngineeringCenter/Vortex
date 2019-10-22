package mil.army.usace.hec.vortex.io;

import java.io.File;

public interface VirtualFileSystem {
    default String getVirtualPath (String pathToArchive){
        // GDAL Virtual path for zip/gzip/tar/tgz files
        String vPath;
        if (pathToArchive.contains(".zip")) {
            vPath = "/vsizip/" + pathToArchive + File.separator;
        } else if (pathToArchive.contains(".gzip")) {
            vPath = "/vsigzip/" + pathToArchive + File.separator;
        } else if (pathToArchive.contains(".tar") || pathToArchive.contains(".tgz")) {
            vPath = "/vsitar/" + pathToArchive + File.separator;
        } else {
            throw new IllegalStateException("File is not *.zip, *.gzip, *.tar, or *.tgz");
        }
        return vPath;
    } // getVirtualPath()
}
