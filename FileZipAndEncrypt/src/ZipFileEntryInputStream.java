import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Provide InputStream access to <b>compressed data</b> from one ZipEntry contained within one ZipFile. Necessary as
 * java.util.zip.ZipInputStream only provides access to the <b>uncompressed data</b>.
 * 
 * @author <a href="mailto:olaf@merkert.de">Olaf Merkert</a>
 */
class ZipFileEntryInputStream extends FileInputStream implements ZipConstants {

    protected long startPos;

    protected long endPos;

    protected long currentPos;

    protected long compressedSize;

    public long getCompressedSize() {
	return this.compressedSize;
    }

    ZipFileEntryInputStream(ZipFile zf) throws IOException {
	super(zf.getName());
    }

    /**
     * position input stream to start of ZipEntry this instance was created for
     * 
     * @throws IOException
     */
    protected void nextEntry(ZipEntry ze) throws IOException {
	this.compressedSize = ze.getCompressedSize();

	super.skip(26); // 18 + compressedSize (4) + size (4)

	byte[] shortBuffer = new byte[2];
	super.read(shortBuffer);
	int fileNameLength = ByteArrayHelper.fromLEByteArray(shortBuffer);

	super.read(shortBuffer);
	int extraFieldLength = ByteArrayHelper.fromLEByteArray(shortBuffer);

	startPos = 18 + 12 + fileNameLength + extraFieldLength;
	currentPos = startPos;
	endPos = startPos + this.compressedSize;

	skip(fileNameLength + extraFieldLength);
    }

    // should work without this, but never trust an OO system
    public int read(byte[] b) throws IOException {
	return this.read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
	int bytesRead = -1;
	int remainingBytes = (int) (endPos - currentPos);
	if (remainingBytes > 0) {
	    if (currentPos + len < endPos) {
		bytesRead = super.read(b, off, len);
		currentPos += bytesRead;
	    } else {
		bytesRead = super.read(b, off, remainingBytes);
		currentPos += bytesRead;
	    }
	}
	return bytesRead;
    }

}
