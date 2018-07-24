import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class Main {

    static int counter = 1;

    /**
     * 1. zipAndEncryptFile
     * 2. renameZipToPDF
     * 3. zipFile
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
	String rootDirLoc = "Z:/Barclays/Good docs/input/";

	File outputDir = new File(rootDirLoc.replace("input", "output"));
	FileUtils.deleteDirectory(outputDir);
//	File outputDir2 = new File(rootDirLoc.replace("input", "output2"));
//	FileUtils.deleteDirectory(outputDir2);
	outputDir.mkdirs();
	File inputDir2 = new File(rootDirLoc.replace("input", "input2"));
	FileUtils.deleteDirectory(inputDir2);

	File rootDir = new File(rootDirLoc);
	// -> output
	zipAndEncryptFiles(rootDir.listFiles());

	// output -> input2
	outputDir.renameTo(inputDir2);

	// rename all files from input2
	renameToPDF(inputDir2.listFiles());

	inputDir2 = new File(rootDirLoc.replace("input", "input2"));

	File outputDir2 = new File(inputDir2.getAbsolutePath().replace("input", "output"));
	outputDir2.mkdirs();

	// compress all files from input2
	zipFiles(inputDir2.listFiles());
	FileUtils.deleteDirectory(inputDir2);

	// output2 -> output
	outputDir2.renameTo(outputDir);
    }

    public static void zipAndEncryptFiles(File[] files) throws IOException {
	for (File file : files) {
	    if (file.isDirectory()) {
		System.out.println("Directory: " + file.getName());
		new File(file.getAbsolutePath().replace("input", "output")).mkdirs();
		zipAndEncryptFiles(file.listFiles()); // Calls same method again.
	    } else {
		System.out.println("Try to zip and encrypt file: " + file.getName());
		AesZipOutputStream.zipAndEcrypt(file.getAbsolutePath(), "Secret007");
	    }
	}
    }

    public static void zipFiles(File[] files) throws IOException {
	for (File file : files) {
	    if (file.isDirectory()) {
		System.out.println("Directory: " + file.getName());
		new File(file.getAbsolutePath().replace("input", "output")).mkdirs();
		zipFiles(file.listFiles()); // Calls same method again.
	    } else {
		System.out.println("Try to zip file: " + file.getName());
		//File newDir = new File(file.getAbsolutePath().replace("input", "output")

		File newFile = new File(file.getAbsolutePath().replace("input", "output") + ".zip");
		//newFile.mkdirs();
		AesZipOutputStream.zip(file, newFile);
	    }
	}
    }

    public static void renameToPDF(File[] files) throws IOException {
	for (File file : files) {
	    if (file.isDirectory()) {
		System.out.println("Directory: " + file.getName());
		renameToPDF(file.listFiles()); // Calls same method again.
	    } else {

		String fileName = file.getParent() + "\\JSP in 24 hrs - " + (counter++) + ".pdf";
		System.out.println("Try to rename file: " + file.getName() + " to =>  " + fileName);

		File newFile = new File(fileName);
		file.renameTo(newFile);
	    }
	}
    }

}
