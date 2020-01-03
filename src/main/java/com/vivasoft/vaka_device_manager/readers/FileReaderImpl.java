package com.vivasoft.vaka_device_manager.readers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Md. Shamim
 * Date: ১৯/১১/১৯
 * Time: ১২:০৪ PM
 * Email: mdshamim723@gmail.com
 **/

@Service
public class FileReaderImpl implements FileReader {

    @Value("${logging-file-path}")
    private String filePath;

    @Override
    public List<String> readLogFile() throws IOException {

        List<String> logs = new ArrayList<>();

        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r");
        String line;
        while ((line = randomAccessFile.readLine()) != null) {
            logs.add(line);
        }

        return logs;
    }

}
