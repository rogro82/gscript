package org.gscript.jni;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class NativeSupport {

	static final String LOG_TAG = "NativeSupport";

	public static boolean prepare(Context context) {

		try {

			/* copy binaries ( for abi ) from asset folder 
			 * to /bin and make them executable */
			
			String abi = android.os.Build.CPU_ABI;
			String dataDir = context.getApplicationInfo().dataDir;
			String abiDir = "bin/" + abi;

			String[] binaries = context.getAssets().list(abiDir);

			/* create bin folder */
			
			File binFolder = new File(dataDir + "/bin");
			if (!binFolder.exists()) {
				binFolder.mkdir();
			}
			
			for (String binary : binaries) {

				File binFile = new File(dataDir + "/bin/" + binary);

				InputStream inputStream = context.getAssets().open(
						abiDir + "/" + binary);
				OutputStream out = new FileOutputStream(binFile);

				int read = 0;
				byte[] bytes = new byte[1024];

				while ((read = inputStream.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}

				inputStream.close();
				out.flush();
				out.close();

				/* TODO: fix for API level 8 */
				binFile.setExecutable(true, false);

				Log.d(LOG_TAG,
						String.format("copied %s [ abi: %s ]", binary, abi));
			}
			
			/* create ipc folder for gscript-input */
			
			File ipcFolder = new File(dataDir + "/ipc");
			if(!ipcFolder.exists()) {
				ipcFolder.mkdir();
			}
			
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

}