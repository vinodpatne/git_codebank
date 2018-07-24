/**
 * byte[] functionality
 * 
 * @author <a href="mailto:olaf@merkert.de">Olaf Merkert</a>
 */
public class ByteArrayHelper {

    public static int fromLEByteArray(byte[] in) {
	int out = 0;

	if (in.length == 4) {
	    out = in[3] & 0xff;
	    out = out << 8;

	    out |= in[2] & 0xff;
	    out = out << 8;
	}

	out |= in[1] & 0xff;
	out = out << 8;

	out |= in[0] & 0xff;

	return out;
    }

    public static byte[] toLEByteArray(int in) {
	byte[] out = new byte[4];

	out[0] = (byte) in;
	out[1] = (byte) (in >> 8);
	out[2] = (byte) (in >> 16);
	out[3] = (byte) (in >> 24);

	return out;
    }

    public static byte[] toLEByteArray(int in, int outSize) {
	byte[] out = new byte[outSize];
	byte[] intArray = toLEByteArray(in);
	for (int i = 0; i < intArray.length && i < outSize; i++) {
	    out[i] = intArray[i];
	}
	return out;
    }

    public static String toString(byte[] theByteArray) {
	StringBuffer theResult = new StringBuffer();
	for (int i = 0; i < theByteArray.length; i++) {
	    theResult.append(Integer.toHexString(theByteArray[i] & 0xff)).append(' ');
	}
	return theResult.toString();
    }

}
