package ca.usask.chl848.rotationsensor;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
/**
 * Created by chl848 on 06/02/2015.
 */
public class MainLogger {
    private BufferedWriter m_bufferedWriter;

    public MainLogger(Context context, String fileName) {
        String SUFFIX = ".csv";
        String targetPath = Environment.getExternalStorageDirectory().getPath()+File.separator+fileName + SUFFIX;
        File targetFile = new File(targetPath);
        if (targetFile != null) {
            if (!targetFile.exists()) {
                try {
                    if (!targetFile.createNewFile())
                    {
                        Toast.makeText(context, "Can not create log file!", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (!targetFile.delete()) {
                    Toast.makeText(context, "Can not delete old log file!", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        if (!targetFile.createNewFile())
                        {
                            Toast.makeText(context, "Can not create log file!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                m_bufferedWriter = new BufferedWriter(new FileWriter(targetPath, true));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void write(String str, boolean isFirstLine) {
        try {
            if (!isFirstLine) {
                m_bufferedWriter.write(System.getProperty("line.separator"));
            }
            m_bufferedWriter.write(str);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                m_bufferedWriter.flush();
                m_bufferedWriter.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void close(){
        try {
            m_bufferedWriter.flush();
            m_bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
