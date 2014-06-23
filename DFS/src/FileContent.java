import java.io.Serializable;

public class FileContent implements Serializable {

	private String data;
	private String fileName;

	public FileContent() {
		this.data = "";
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public void append(String update) {
		data += update;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}
}
