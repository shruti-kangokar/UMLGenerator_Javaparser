
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.sourceforge.plantuml.SourceStringReader;

public class generateUML {
	public void generateUml(String source, String destination) {

		OutputStream jpeg = null;
		try {
			jpeg = new FileOutputStream(destination);
			System.out.println("Package");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SourceStringReader reader = new SourceStringReader(source);
		// Write the first image to "jpeg"
		try {
			reader.generateImage(jpeg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Return a null string if no generation
	}
}
