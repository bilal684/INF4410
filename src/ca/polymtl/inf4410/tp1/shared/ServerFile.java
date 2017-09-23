package ca.polymtl.inf4410.tp1.shared;

import java.io.Serializable;

public class ServerFile implements Serializable {
	
	private static final long serialVersionUID = 361417043140836160L;
	private String fileName;
	private byte[] content;
	
	public ServerFile(String fileName, byte[] content)
	{
		this.fileName = fileName;		
		this.content = content;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}
}
