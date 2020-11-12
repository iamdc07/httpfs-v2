import model.Config;
import model.ServerParameters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HttpResponse {
    public void processResponse(ServerParameters serverParameters) {
        if (Config.isVerbose)
            System.out.println("Processing Response");

        if (serverParameters.requestType.equalsIgnoreCase("GET")) {
            serveGetRequest(serverParameters);
        } else {
            servePostRequest(serverParameters);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public void serveGetRequest(ServerParameters serverParameters) {
        FileOperations fileOperations = new FileOperations();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        fileOperations.processFileOperation(serverParameters);

        String response = "";


        if (serverParameters.hasFileList) {
            response = response.concat("HTTP/1.0 " + ErrorCodes.SUCCESS + "\r\n");
        } else if (serverParameters.hasData) {
            response = response.concat("HTTP/1.0 " + ErrorCodes.SUCCESS + "\r\n");
        } else {
            response = response.concat("HTTP/1.0 " + ErrorCodes.NOT_FOUND + "\r\n");
        }

        response = response.concat("Date: " + dtf.format(now) + "\r\n");
        response = response.concat("Content-Length: " + serverParameters.data.length() + "\r\n");
        response = response.concat("Connection: " + "close" + "\r\n");

        if (serverParameters.isContentDisposition) {
            if (serverParameters.isInline)
                response = response.concat("Content-Disposition: inline\r\n");
            else
                response = response.concat("Content-Disposition:" + " attachment; filename = test.txt\r\n");
        }

        response = response.concat("Server: httpfs/v1.0" + "\r\n");
        response = response.concat("\r\n");

        if (serverParameters.hasFileList)
            response = response.concat(serverParameters.fileList);
        else if (serverParameters.hasData)
            response = response.concat(serverParameters.data);

        serverParameters.response = response;

        if (Config.isVerbose)
            System.out.println("Generated Response for Get Response, preparing to send...\n");
    }

    @SuppressWarnings("DuplicatedCode")
    public void servePostRequest(ServerParameters serverParameters) {
        FileOperations fileOperations = new FileOperations();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        fileOperations.processFileOperation(serverParameters);

        String response = "";

        if (serverParameters.postSuccess) {
            response = response.concat("HTTP/1.0 " + ErrorCodes.SUCCESS + "\r\n");
        } else {
            response = response.concat("HTTP/1.0 " + ErrorCodes.BAD_REQUEST + "\r\n");
        }

        response = response.concat("Date: " + dtf.format(now) + "\r\n");
        response = response.concat("Content-Length: " + serverParameters.data.length() + "\r\n");
        response = response.concat("Connection: " + "close" + "\r\n");
        response = response.concat("Server: httpfs/v1.0" + "\r\n");
        response = response.concat("\r\n");

        if (serverParameters.hasData)
            response = response.concat(serverParameters.data);

        serverParameters.response = response;

        if (Config.isVerbose)
            System.out.println("Generated Response for Post Request, preparing to send...\n");
    }
}