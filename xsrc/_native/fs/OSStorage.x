import Ecstasy.fs.Directory;
import Ecstasy.fs.File;
import Ecstasy.fs.FileStore;
import Ecstasy.fs.Path;

/**
 * Native OSStorage service.
 */
service OSStorage
    {
    construct()
        {
        }
    finally
        {
        fileStore = new OSFileStore(this, false);
        }

    @Unassigned
    FileStore fileStore;

    Directory rootDir.get()
        {
        return fileStore.root;
        }

    // ----- native --------------------------------------------------------------------------------

    @Abstract @RO Directory homeDir;
    @Abstract @RO Directory curDir;
    @Abstract @RO Directory tmpDir;

    conditional Directory|File find(OSFileStore store, String pathString);

    String[] names(OSDirectory dir);

    Boolean createDir(OSFileStore store, String path);

    Boolean createFile(OSFileStore store, String path);

    Boolean delete(OSFileStore store, String path);
    }
