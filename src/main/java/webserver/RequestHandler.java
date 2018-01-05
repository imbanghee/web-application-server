package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	
	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
		
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String line = br.readLine();
			log.debug("request line : {}",line);
			String[] tokens = line.split(" ");
			for (String token : tokens) {
				log.debug("token : {}",token);
			}
			
			Map<String, String> headerMap = new HashMap<String, String>();
			while(!"".equals(line)) {
				line = br.readLine();
				log.debug("header : {}",line);
				String[] headerToken = line.split(": ");
				if(headerToken.length == 2){
					headerMap.put(headerToken[0], headerToken[1]);
				}
			}
			
			String acceptHeader = headerMap.get("Accept");
			String method = tokens[0];
			String url = tokens[1];
			String params;
			if(url.startsWith("/create") || url.startsWith("/login")){
				Map<String,String> param=null;
				if(method.equals("GET")){
					int index = url.indexOf("?");
					String requestPath = url.substring(0, index);
					params = url.substring(index+1);
				}else{
					params = IOUtils.readData(br, Integer.parseInt(headerMap.get("Content-Length")));
				}
				
				log.debug("params : {}",params);
				param = HttpRequestUtils.parseQueryString(params);
				
				User user = new User(param.get("userId"), param.get("password"), param.get("name"), param.get("email"));
				log.debug("userinfo : {}",user.toString());
				
				DataOutputStream dos = new DataOutputStream(out);
				response302Header(dos);
			}else{
			
				String filePath = "./webapp"+url;
				byte[] body = Files.readAllBytes(new File(filePath).toPath());
				
				DataOutputStream dos = new DataOutputStream(out);
	//			byte[] body = "Hello World".getBytes();
				response200Header(dos, acceptHeader, body.length);
				responseBody(dos, body);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void response200Header(DataOutputStream dos, String acceptHeader, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: "+acceptHeader+";charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void response302Header(DataOutputStream dos) {
		try {
			dos.writeBytes("HTTP/1.1 302 Found \r\n");
			dos.writeBytes("Location: ./index.html \r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	
	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.writeBytes("\r\n");
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}