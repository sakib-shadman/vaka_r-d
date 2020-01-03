package com.vivasoft.vaka_device_manager.readers;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Md. Shamim
 * Date: ১৯/১১/১৯
 * Time: ১১:৫৮ AM
 * Email: mdshamim723@gmail.com
 **/

public interface FileReader {

    public List<String > readLogFile() throws IOException;

}
