import java.util.Calendar;
import java.util.Date;
import java.util.zip.ZipEntry;

/**
 * information about one zip entry that is written to the encrypted zip archive
 * 
 * @author <a href="mailto:olaf@merkert.de">Olaf Merkert</a>
 */
public class AesZipEntry extends ZipEntry {

    public AesZipEntry(ZipEntry zipEntry) {
	super(zipEntry.getName());
	super.setMethod(zipEntry.getMethod());
	super.setSize(zipEntry.getSize());
	super.setCompressedSize(zipEntry.getCompressedSize() + 28);
	super.setTime(zipEntry.getTime());

	flag |= 1; // bit0 - encrypted
	// flag |= 8; // bit3 - use data descriptor
    }

    public boolean useDataDescriptor() {
	return ((flag & 8) == 8);
    }

    protected int flag;

    public int getFlag() {
	return this.flag;
    }

    protected int offset;

    public int getOffset() {
	return offset;
    }

    public void setOffset(int offset) {
	this.offset = offset;
    }

    // --------------------------------------------------------------------------

    public long getDosTime() {
	return javaToDosTime(getTime());
    }

    protected static long javaToDosTime(long time) {
	Date d = new Date(time);
	Calendar ca = Calendar.getInstance();
	ca.setTime(d);
	int year = ca.get(Calendar.YEAR);
	if (year < 1980) {
	    return (1 << 21) | (1 << 16);
	}
	return (year - 1980) << 25 | (ca.get(Calendar.MONTH) + 1) << 21 | ca.get(Calendar.DAY_OF_MONTH) << 16
		| ca.get(Calendar.HOUR_OF_DAY) << 11 | ca.get(Calendar.MINUTE) << 5 | ca.get(Calendar.SECOND) >> 1;
    }

}
