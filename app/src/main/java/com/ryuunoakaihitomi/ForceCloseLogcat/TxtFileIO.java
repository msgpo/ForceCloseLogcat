package com.ryuunoakaihitomi.ForceCloseLogcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 对文本文件的操作
 */

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TxtFileIO {

    //删
    static void D(String path) {
        DKernel(new File(path));
    }

    //写
    static void W(String fileName, String txtBody) {
        try {
            File file = new File(fileName);
            File dir = new File(fileName.subSequence(0, fileName.lastIndexOf("/")).toString());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file);
            byte[] bytes = (txtBody == null ? "" : txtBody).getBytes();
            fos.write(bytes);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //读
    static String R(String fileName) {
        String res = "";
        try {
            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            int length = fis.available();
            byte[] buffer = new byte[length];
            fis.read(buffer);
            res = new String(buffer, "UTF-8");
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    //删(内部)
    private static void DKernel(File f) {
        if (f.isFile()) {
            f.delete();
            return;
        }
        if (f.isDirectory()) {
            File[] childFiles = f.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                f.delete();
                return;
            }
            for (File childFile : childFiles) {
                DKernel(childFile);
            }
            f.delete();
        }
    }

}
