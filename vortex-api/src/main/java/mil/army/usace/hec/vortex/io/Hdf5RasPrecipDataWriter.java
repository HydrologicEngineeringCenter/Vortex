package mil.army.usace.hec.vortex.io;

import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import mil.army.usace.hec.vortex.VortexGrid;
import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Hdf5RasPrecipDataWriter extends DataWriter {

    private static final Logger logger = Logger.getLogger(Hdf5RasPrecipDataWriter.class.getName());

    Hdf5RasPrecipDataWriter(Builder builder) {
        super(builder);
    }

    @Override
    public void write() {
        List<VortexGrid> grids = data.stream()
                .filter(vortexData -> vortexData instanceof VortexGrid)
                .map(vortexData -> (VortexGrid) vortexData)
                .collect(Collectors.toList());

        if (grids.size()>0) {
            try (HdfRasPrecipDataset dset = new HdfRasPrecipDataset(grids, this.options)) {
                dset.open(this.destination);
                dset.copy();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
    ------------------------------------------------------------------------------
    Logic for writing HDF5 precip dataset is encapsulated in the inner class below
    ------------------------------------------------------------------------------
    */
    private interface HdfCloser{
        public void close() throws HDF5LibraryException;
    }

    private class HdfRasPrecipDataset implements AutoCloseable{
        private int file_id = HDF5Constants.H5I_INVALID_HID;
        private int filespace_id=-1;
        private int dataset_id=-1;
        private int dcpl_id=-1;
        private int memoryspace_id=-1;
        private final List<VortexGrid> grids;
        private final int RANK = 2, NDIMS = 2;
        private final long ROWCHUNK = 1;
        private long[] chunk_dims;

        private final int cols;
        private int lastrow = 0;
        private final String dataset_name;
        private final String DefaultDatasetName = "Event Conditions/Meteorology/Precipitation/Values";

        private final int max_rows = 999999;

        public HdfRasPrecipDataset(List<VortexGrid> grids, Map<String,String> options) throws Exception{
            this.grids=grids;
            this.dataset_name = options.getOrDefault("dataset_name", DefaultDatasetName);
            this.cols = grids.get(0).nx()*grids.get(0).ny();
            this.chunk_dims= new long[]{ROWCHUNK, cols};
        }

        public void open(Path path) throws Exception {
            File f = new File(path.toString());
            if(f.exists()){
                this.file_id = H5.H5Fopen(path.toString(),
                        HDF5Constants.H5F_ACC_RDWR,
                        HDF5Constants.H5P_DEFAULT);

            } else {
                this.file_id = H5.H5Fcreate(path.toString(),
                        HDF5Constants.H5F_ACC_EXCL,
                        HDF5Constants.H5P_DEFAULT,
                        HDF5Constants.H5P_DEFAULT);
            }
            openDataset();
        }

        private void openDataset() throws Exception{

            long[] dims = new long[2];
            long[] maxdims = new long[2];

            if (exists(this.dataset_name)){
                logger.finest(String.format("Found existing dataset: %s in file.  Opening dataset for write.",this.dataset_name));
                this.dataset_id = H5.H5Dopen(this.file_id, this.dataset_name, HDF5Constants.H5P_DEFAULT);

                //dataset exists so we need to extend the dataset.
                //to do this we get the current dimensions
                //and add the number of grids sent to the writer
                int space_id = H5.H5Dget_space(dataset_id);
                H5.H5Sget_simple_extent_dims(space_id, dims, maxdims);
                this.lastrow=(int)dims[0];
                dims[0]=dims[0]+this.grids.size();
                H5.H5Dset_extent(this.dataset_id,dims);

                //finally we need to update the filespace reference with the new dimension
                this.filespace_id = H5.H5Screate_simple(RANK, dims, maxdims);

            } else {
                logger.finest(String.format("Did not find dataset: %s in file.  Creating dataset for write.",this.dataset_name));
                createPath(this.dataset_name);
                dims = new long[]{this.grids.size(), cols};
                maxdims = new long[]{max_rows,cols};
                this.filespace_id = H5.H5Screate_simple(RANK, dims, maxdims);

                //build the dataset creation options
                //consistent with RAS, we chunk the dataset by rows
                //and use compression with a deflate value of 1
                this.dcpl_id = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
                if (this.dcpl_id >= 0) {
                    H5.H5Pset_deflate(this.dcpl_id, 1);
                    H5.H5Pset_chunk(this.dcpl_id, NDIMS, chunk_dims);
                }

                //if everything is ok, create the dataset
                //if not an error 'should' have been thrown,
                //so I don't think the checking for valid IDs is necessary,
                //however, it's recommended in the java examples.
                if ((this.file_id >= 0) && (this.filespace_id >= 0) && (this.dcpl_id >= 0)) {
                    this.dataset_id = H5.H5Dcreate(this.file_id, this.dataset_name, HDF5Constants.H5T_IEEE_F32LE, filespace_id, HDF5Constants.H5P_DEFAULT, this.dcpl_id, HDF5Constants.H5P_DEFAULT);
                } else{
                    throw new HDF5Exception(String.format("Unable to create dataset: %s",this.dataset_name));
                }
            }
        }

        public void copy() throws Exception{
            int i = 0;
            long[] stride = {1, 1};
            long[] block  = {1, 1};

            //create a memory space representing the dimension of the vortex grid data we are copying to hdf5
            this.memoryspace_id = H5.H5Screate_simple(2,this.chunk_dims,null);

            for (VortexGrid grid : grids) {
                long[] start = {this.lastrow + i, 0};
                long[] count = {1, cols};
                float[] transposedData = transposeVerticalAndReplaceNodata(grid);
                try {
                    //select the location in the hdf5 dataset to copy the new data into
                    H5.H5Sselect_hyperslab(this.filespace_id, HDF5Constants.H5S_SELECT_SET, start, stride, count, block);

                    //write the transposed vortex grid data to hdf5
                    H5.H5Dwrite(this.dataset_id, HDF5Constants.H5T_IEEE_F32LE, this.memoryspace_id, this.filespace_id, HDF5Constants.H5P_DEFAULT, transposedData);
                } catch (HDF5Exception e) {
                    //can't throw a checked exception in a foreach
                    //so throw a runtime exception.
                    throw new RuntimeException(e);
                }
                i++;
            }

        }


        /*
            using a 'closer' interface to close hdf5 resources
            hdf5 will throw checked exceptions so as far as I
            can tell, to be certain you closed all resources
            you need to wrap all calls in a try/catch
        */
        @Override
        public void close() throws Exception {

            if (dcpl_id > -1) {
                closer(()-> H5.H5Pclose(dcpl_id));
            }

            if (dataset_id > -1){
                closer(()->H5.H5Dclose(dataset_id));
            }

            if (memoryspace_id > -1){
                closer(()->H5.H5Sclose(memoryspace_id));
            }

            if (filespace_id > -1){
                closer(()->H5.H5Sclose(filespace_id));
            }

            if (this.file_id > -1) {
                closer(()-> H5.H5Fclose(this.file_id));
            }

            logger.info("Successfully closed HDF5 resources.");

        }

        //enumerates all parts in a path to determine if they exist in the hdf5 file
        private boolean exists(String path) throws Exception {
            String root="";
            List<String> pathparts = new ArrayList<String>(Arrays.asList(path.split("/")));
            return datasetExists(pathparts,root);
        }

        private boolean datasetExists(List<String> pathparts, String root) throws Exception{
            for (String pathpart:pathparts){
                String pathtest=root+"/"+pathpart;
                if (H5.H5Lexists(this.file_id,pathtest,HDF5Constants.H5P_LINK_ACCESS_DEFAULT)){
                    pathparts.remove(0);
                    if (pathparts.size()==0){
                        return true;
                    } else {
                        return datasetExists(pathparts,pathtest);
                    }
                } else {
                    return false;
                }
            }
            return false;
        }

        //creates the path or parts that are missing for the hdf5 file dataset
        private void createPath(String dataset_name) throws Exception{
            int gcpl_id = -1;
            int group_id = -1;
            String path = dataset_name.substring(0,dataset_name.lastIndexOf("/"));
            if(!exists(path)){
                try{
                    gcpl_id = H5.H5Pcreate(HDF5Constants.H5P_LINK_CREATE);
                    H5.H5Pset_create_intermediate_group(gcpl_id, true);
                    group_id = H5.H5Gcreate(this.file_id, path, gcpl_id, HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
                }
                catch(Exception ex){
                    logger.severe(String.format("Errror creating group path: %s",ex.getMessage()));
                }
                finally {
                    if (gcpl_id > -1) {
                        final int finalGcpl_id = gcpl_id;
                        closer(()-> H5.H5Pclose(finalGcpl_id));
                    }
                    if (group_id > -1){
                        final int finalGroup_id = group_id;
                        closer(()->H5.H5Gclose(finalGroup_id));
                    }
                }
            }
        }

        private void closer(HdfCloser hc){
            try{
                hc.close();
            } catch (Exception ex){
                logger.severe("Failed to close hdf resource:" + ex.getMessage());
            }
        }

        // transposes data vertically and replaces values <0 with 0
        private float[] transposeVerticalAndReplaceNodata(VortexGrid grid) {
            float[] data = grid.data();
            float[] transposedData = new float[data.length];
            int rows = grid.ny();
            int cols = grid.nx();
            for (int r = 0; r < rows; r++) {
                int readindex = r * cols;
                int writeindex = (rows - r - 1) * cols;
                for (int c = 0; c < cols; c++) {
                    float val = data[readindex+c];
                    transposedData[writeindex + c] = (val<0)?0:val; //setting all values <0 to 0 rather than only values matching a specific negative NODATA value
                }
            }
            return transposedData;
        }
    }
}