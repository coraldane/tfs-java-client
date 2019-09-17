package com.taobao.common.tfs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.taobao.common.tfs.impl.TfsFile;

public class TfsBaseCase {
	public static final Logger log = LoggerFactory.getLogger(TfsBaseCase.class);

	private static ClassPathXmlApplicationContext appContext;
	public static TfsManager tfsManager;

	public static String resource_file = null;
	public static String resource_file_big = null;
	public static String resource_file_small = null;
	public static String TfsName_local = null;
	public static String resource = null;
	public final int ERRTFSNAMEEMPTY = -1;
	public final int ERRTFSNAMENULL = -2;
	public final int ERRTFSNAME = -3;
	public final int ERRTFSSAVE = -4;
	public final int ERRTFSCRC = -5;
	public final int ERRINFONULL = -6;
	public final int ERRTFSSTAT = -7;
	public final int ERRTFSSTATHIDE = -8;
	public final int ERRTFSSTATDEL = -9;
	public final int ERRHIDE = -10;
	public final int ERRREHIDE = -11;
	public final int ERRDEL = -12;
	public final int INFOSUCCESS = 0;
	public static List<String> serverList = new ArrayList<String>();
	public final int LARGE = 1;
	public final int SMALL = 0;
	public final int BOTH = 2;
	public final String RESPATH = "src/test/resources/";
	private RandomAccessFile file;
	private int readSize = 1024 * 1024;
	private int[] FILESIZELIST = { 1, 1024 * 1024, TfsFile.MAX_SMALL_FILE_LENGTH - 1, TfsFile.MAX_SMALL_FILE_LENGTH,
			1024 * 1024 * 1024, 10 * 1024 * 1024 };
	private String[] FILENAMELIST = { "1.jpg", "1m.jpg", "MAX_SMALL_FILE_SIZE-1.jpg", "MAX_SMALL_FILE_SIZE.jpg",
			"1g.jpg", "10M.jpg" };

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		appContext = new ClassPathXmlApplicationContext(new String[] { "tfs.xml" });
		tfsManager = (DefaultTfsManager) appContext.getBean("tfsManager");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		tfsManager.destroy();
		appContext.close();
	}

	static {
		ClassPathResource resource_path = new ClassPathResource("100k.jpg");
		try {
			resource_file = resource_path.getFile().getAbsolutePath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resource_path = new ClassPathResource("200k.jpg");
		try {
			resource_file_big = resource_path.getFile().getAbsolutePath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resource_path = new ClassPathResource("15k.jpg");
		try {
			resource_file_small = resource_path.getFile().getAbsolutePath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TfsName_local = "LocalFile.jpg";
		resource = "100k.txt";
	}

	public TfsBaseCase() {
		int iRet = -1;
		for (int iLoop = 0; iLoop < FILESIZELIST.length; iLoop++) {
			iRet = createFile(RESPATH + FILENAMELIST[iLoop], FILESIZELIST[iLoop]);
			if (iRet != 0) {
				log.error("File(" + FILENAMELIST[iLoop] + " " + FILESIZELIST[iLoop] + ") is failed to create!!!");
			}
		}
	}

	public void pathInit() {
		ClassPathResource resource_path = new ClassPathResource("100k.jpg");
		try {
			resource_file = resource_path.getFile().getAbsolutePath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resource_path = new ClassPathResource("200k.jpg");
		try {
			resource_file_big = resource_path.getFile().getAbsolutePath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resource_path = new ClassPathResource("15k.jpg");
		try {
			resource_file_small = resource_path.getFile().getAbsolutePath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TfsName_local = "LocalFile.jpg";
		resource = "100k.txt";
	}

	public void deleteFile(String fileName) {
		File file = new File(fileName);
		if (file.exists() && file.isFile()) {
			file.delete();
			// System.out.println("delete file: "+fileName);
			log.info("delete file: " + fileName);
			return;
		}
		// System.out.println("unable to delete file: "+fileName+", file does
		// not exist!");
		log.info("unable to delete file: " + fileName + ", file does not exist!");
		return;
	}

	public CRC32 getTfsCrc(String tfsFile, String suffix, int iFlag) {
		CRC32 crc = new CRC32();
		int fd = -1;
		Random rd = new Random();
		int readLen = -1;
		ByteArrayOutputStream opStream = new ByteArrayOutputStream();
		int len = Math.abs(rd.nextInt()) % (1024 * 1024 * 100);
		byte[] buff = new byte[len];

		crc.reset();

		if (iFlag == LARGE) {
			/* Large file */
			fd = tfsManager.openReadFile(tfsFile, suffix);
			if (fd <= 0) {
				log.error("The handle of file error : fd = " + fd);
				return null;
			}

			/* Read file */
			while (true) {
				// read data
				if ((readLen = tfsManager.readFile(fd, buff, 0, len)) < 0) {
					log.error("Read large file failed!!!");
					return null;
				}
				crc.update(buff, 0, readLen);
				if (readLen < len) {
					break;
				}
			}

		} else {
			/* Small file */
			boolean bRet = tfsManager.fetchFile(tfsFile, suffix, opStream);
			if (bRet == false) {
				log.error("Small file is failed to read!!!");
				return null;
			}
			crc.update(opStream.toByteArray());
		}

		return crc;
	}

	public int checkTfsName(String tfsFile, int iFlag) {
		if (tfsFile == null) {
			log.error("Tfs name which is returned is null!!!");
			return ERRTFSNAMENULL;
		}

		/* Check tfsFile */
		if (tfsFile.equals("")) {
			log.error("Tfs name which is returned is empty!!!");
			return ERRTFSNAMEEMPTY;
		}

		if (iFlag == BOTH) {
			return INFOSUCCESS;
		}

		if (iFlag == LARGE) {
			if (tfsFile.charAt(0) != 'L') {
				log.error("Large file's tfs name(" + tfsFile + ") is not begin with L!!!");
				return ERRTFSNAME;
			}
		} else {
			if (tfsFile.charAt(0) != 'T') {
				log.error("Small file's tfs name(" + tfsFile + ") is not begin with T!!!");
				return ERRTFSNAME;
			}
		}
		return INFOSUCCESS;
	}

	public void setReadSize(int readSize) {
		this.readSize = readSize;
		return;
	}

	public int createFile(String path, long fileSize) {
		try {
			file = new RandomAccessFile(path, "rw");
			file.setLength(fileSize);
		} catch (FileNotFoundException e) {
			log.error("", e);
			return -1;
		} catch (IOException e) {
			log.error("", e);
			return -1;
		} finally {
			if (file != null) {
				try {
					file.close();
				} catch (IOException e) {
					log.error("", e);
					return -1;
				}
			}
		}
		return 0;
	}

	public CRC32 getCrc(String fileName) {
		CRC32 retCrc = new CRC32();
		char[] readBuff = new char[readSize];
		int iBuff = -1;
		FileReader fR;

		/* Reset */
		retCrc.reset();

		/* Get the data from local file */
		try {
			fR = new FileReader(fileName);
		} catch (Exception e) {
			log.error("", e);
			return null;
		}
		try {
			while ((iBuff = fR.read(readBuff)) != -1) {
				retCrc.update(String.valueOf(readBuff).getBytes(), 0, iBuff);
			}
		} catch (IOException e) {
			log.error("", e);
			return null;
		} finally {
			if(null != fR){
				try {
					fR.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return retCrc;
	}

}
