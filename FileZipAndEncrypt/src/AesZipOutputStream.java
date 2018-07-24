import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Create ZIP-Outputstream containing entries from an existing ZIP-File, but AES encrypted.
 *
 * @author <a href="mailto:olaf@merkert.de">Olaf Merkert</a>
 */
public class AesZipOutputStream implements ZipConstants {

    protected OutputStream out;

    protected AesZipOutputStream(File file) throws IOException {
	out = new FileOutputStream(file);
    }

    protected AesZipOutputStream(OutputStream out) {
	this.out = out;
    }

    protected void add(ZipFile inFile, String password) throws IOException, UnsupportedEncodingException {
	ZipFileEntryInputStream zfe = new ZipFileEntryInputStream(inFile);
	Enumeration en = inFile.entries();
	while (en.hasMoreElements()) {
	    ZipEntry ze = (ZipEntry) en.nextElement();
	    zfe.nextEntry(ze);
	    add(ze, zfe, password);
	}
	zfe.close();
    }

    protected void add(ZipEntry zipEntry, ZipFileEntryInputStream zipData, String password) throws IOException,
	    UnsupportedEncodingException {
	AESEncrypter aesEncrypter = new AESEncrypter(password.getBytes("iso-8859-1"));

	AesZipEntry entry = new AesZipEntry(zipEntry);

	putNextEntry(entry);
	/*
	 * ZIP-file data contains: 1. salt 2. pwVerification 3. ecnryptedContent 4. authenticationCode
	 */
	writeBytes(aesEncrypter.getSalt());
	writeBytes(aesEncrypter.getPwVerification());

	byte[] data = new byte[1024];
	int read = zipData.read(data);
	while (read != -1) {
	    aesEncrypter.encrypt(data, read);
	    writeBytes(data, 0, read);
	    read = zipData.read(data);
	}

	writeBytes(aesEncrypter.getFinalAuthentication());
    }

    protected void putNextEntry(AesZipEntry entry) throws IOException {
	entries.add(entry);

	entry.setOffset(written);

	// file header signature
	writeInt(LOCSIG);

	writeFileInfo(entry);
	writeBytes(entry.getName().getBytes("iso-8859-1"));
	writeExtraBytes(entry);
    }

    private List<AesZipEntry> entries = new ArrayList<AesZipEntry>();

    private final static short ZIP_VERSION = 20; // version set by java.util.zip

    private void writeDirEntry(AesZipEntry entry) throws IOException {
	writeInt(CENSIG); // writeBytes( new byte[] { 0x50, 0x4b, 0x01, 0x02 }
	// ); // directory signature
	writeShort(ZIP_VERSION); // version made by
	writeFileInfo(entry);

	writeShort(0x00); // file comment length 2 bytes
	writeShort(0x00); // disk number start (unused) 2 bytes
	writeShort(0x00); // internal file attributes (unsued) 2 bytes
	writeInt(0x00); // external file attributes (unused) 4 bytes

	writeInt(entry.getOffset()); // relative offset of local header 4 bytes

	writeBytes(entry.getName().getBytes("iso-8859-1"));

	writeExtraBytes(entry);
    }

    private void writeFileInfo(AesZipEntry entry) throws IOException {
	writeShort(ZIP_VERSION); // version needed to extract

	// general purpose bit flag - 0x0001 indicates encryption 2 bytes
	writeShort(entry.getFlag());

	writeShort(0x63); // primary compression method - 0x63==encryption

	writeInt(entry.getDosTime());
	/*
	 * writeBytes( new byte[] { (byte)0x5b, (byte)0x65 } ); // last mod file time writeBytes( new byte[] {
	 * (byte)0x2d, (byte)0x35 } ); // last mod file date
	 */

	writeInt(0x00); // CRC-32 / for encrypted files it's 0 as AES/MAC checks
	// integritiy

	// 28 bytes is the encryption overhead (caused by 256-bit AES key)
	// 2 bytes pwVerification + 16 bytes SALT + 10 bytes AUTHENTICATION

	writeInt((int) entry.getCompressedSize()); // compressed size
	writeInt((int) entry.getSize()); // uncompressed size

	writeShort(entry.getName().length()); // file name length
	writeShort(0x0b); // extra field length
    }

    private void writeExtraBytes(ZipEntry entry) throws IOException {
	byte[] extraBytes = new byte[11];
	extraBytes[0] = 0x01;
	extraBytes[1] = (byte) 0x99;

	extraBytes[2] = 0x07; // data size
	extraBytes[3] = 0x00; // data size

	extraBytes[4] = 0x02; // version number
	extraBytes[5] = 0x00; // version number

	extraBytes[6] = 0x41; // vendor id
	extraBytes[7] = 0x45; // vendor id

	extraBytes[8] = 0x03; // AES encryption strength - 1=128, 2=192, 3=256

	// 41 45 03

	// actual compression method - 0x0000==stored (no compression) - 2 bytes
	extraBytes[9] = (byte) (entry.getMethod() & 0xff);
	extraBytes[10] = (byte) ((entry.getMethod() & 0xff00) >> 8);
	writeBytes(extraBytes);
    }

    /**
     * Finishes writing the contents of the ZIP output stream without closing the underlying stream. Also closes the
     * stream.
     */
    protected void finish() throws IOException {
	int dirOffset = written; // central directory (at end of zip file)
	// starts here

	int startOfCentralDirectory = written;

	Iterator it = entries.iterator();
	while (it.hasNext()) {
	    AesZipEntry entry = (AesZipEntry) it.next();
	    writeDirEntry(entry);
	}
	int centralDirectorySize = written - startOfCentralDirectory;

	writeInt(ENDSIG); // writeBytes( new byte[] { 0x50, 0x4b, 0x05, 0x06 }
	// ); // end of central dir signature 4 bytes
	// (0x06054b50)

	writeShort(0x00); // number of this disk 2 bytes
	writeShort(0x00); // number of the disk with the start of the central
	// directory 2 bytes

	writeShort(entries.size()); // total number of entries in central
	// directory on this disk 2 bytes
	writeShort(entries.size()); // total number of entries in the central
	// directory 2 bytes

	writeInt(centralDirectorySize); // size of the central directory 4 bytes

	writeInt(dirOffset); // offset of start of central directory with
	// respect to the starting disk number 4 bytes
	writeShort(0x00); // .ZIP file comment length 2 bytes

	out.close();
    }

    // --------------------------------------------------------------------------

    /** number of bytes written to out */
    protected int written;

    protected void writeBytes(byte[] b) throws IOException {
	out.write(b);
	written += b.length;
    }

    protected void writeShort(int v) throws IOException {
	out.write((v >>> 0) & 0xff);
	out.write((v >>> 8) & 0xff);
	written += 2;
    }

    protected void writeInt(long v) throws IOException {
	out.write((int) ((v >>> 0) & 0xff));
	out.write((int) ((v >>> 8) & 0xff));
	out.write((int) ((v >>> 16) & 0xff));
	out.write((int) ((v >>> 24) & 0xff));
	written += 4;
    }

    protected void writeBytes(byte[] b, int off, int len) throws IOException {
	out.write(b, off, len);
	written += len;
    }

    // --------------------------------------------------------------------------

    /**
     * Compress (zip) inFile stored in created outFile. If you need multiple files added to outFile use java's
     * ZipOutStream directly.
     */
    public static void zip(File inFile, File outFile) throws IOException {
	FileInputStream fin = new FileInputStream(inFile);
	FileOutputStream fout = new FileOutputStream(outFile);
	ZipOutputStream zout = new ZipOutputStream(fout);

	zout.putNextEntry(new ZipEntry(inFile.getName()));
	byte[] buffer = new byte[1024];
	int len;
	while ((len = fin.read(buffer)) > 0) {
	    zout.write(buffer, 0, len);
	}
	zout.closeEntry();

	zout.close();
	fin.close();
    }

    /**
     * encrypt zip file contents - encrypted data has the same size as the compressed data, though the file size is
     * increased by 26 bytes for salt and pw-verification bytes
     *
     * @param pathName
     *            path to file inclusing filename but NOT file extension (is always ".zip")
     * @param password
     *            used to perform the encryption
     * @throws IOException
     */
    public static void encrypt(String pathName, String password) throws IOException {
	AesZipOutputStream zos = new AesZipOutputStream(new File(pathName.replace("input", "output") + ".zip"));
	ZipFile zipFile = new ZipFile(pathName + "_temp" + ".zip");
	zos.add(zipFile, password);
	zos.finish();
	zipFile.close();
    }

    public static void zipAndEcrypt(String pathName, String extName, String password) throws IOException {
	File inFile = new File(pathName + "." + extName);
	File outFile = new File(pathName + ".zip");
	zip(inFile, outFile);
	encrypt(pathName, password);
    }

    public static void zipAndEcrypt(String filePath, String password) throws IOException {
	File inFile = new File(filePath);
	String fileName = inFile.getAbsolutePath();
	int lastPos = fileName.lastIndexOf(".");
	String pathName = fileName.substring(0, lastPos);
	//System.out.println(pathName);
	File outFile = new File(pathName + "_temp" + ".zip");
	zip(inFile, outFile);
	encrypt(pathName, password);
	outFile.delete();
    }

}
