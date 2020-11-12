import model.Config;
import model.ServerParameters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HttpRequest {

    public void processRequest(StringBuilder sb, ServerParameters serverParameters) {
        String incoming = sb.toString();
        String[] lines = incoming.split("\r\n");

        for (int i = 0; i < lines.length; i++) {
            String[] words = lines[i].split(" ");

            if (i == 0) {
                serverParameters.requestType = words[0];
                if (words.length == 3) {
                    serverParameters.filename = serverParameters.filename + words[1];
                } else if (words.length == 2) {
                    serverParameters.filename = serverParameters.filename + "/";
                }
            }

            if (words[0].equalsIgnoreCase("Content-Disposition:")) {
                serverParameters.isContentDisposition = true;

                if (words[1].equalsIgnoreCase("inline"))
                    serverParameters.isInline = true;
            }

            if (words[0].equalsIgnoreCase("Content-Type:")) {
                if (words[1].equalsIgnoreCase("application/json")) {
                    serverParameters.extension = ".json";
                } else if (words[1].equalsIgnoreCase("application/xml")) {
                    serverParameters.extension = ".xml";
                } else if (words[1].equalsIgnoreCase("application/html")) {
                    serverParameters.extension = ".html";
                }
            }

            if (lines[i].equalsIgnoreCase("") && i != lines.length - 1) {
                serverParameters.payload = lines[i + 1];
            }
        }

        if (Config.isVerbose)
            System.out.println("Request Processed\n");

        if (FileOperations.validatePath(serverParameters)) {
            if (Config.isVerbose)
                System.out.println("Successfully Validated File Path\n");

            HttpResponse httpResponse = new HttpResponse();
            httpResponse.processResponse(serverParameters);
        } else {
            if (Config.isVerbose)
                System.out.println("Invalid Access made by Client, sending Error Response\n");
            errorResponse(serverParameters);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public void errorResponse(ServerParameters serverParameters) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        String response = "";

        response = response.concat("HTTP/1.0 " + ErrorCodes.BAD_REQUEST + "\r\n");
        response = response.concat("Date: " + dtf.format(now) + "\r\n");
        response = response.concat("Content-Length: " + serverParameters.data.length() + "\r\n");
        response = response.concat("Connection: " + "close" + "\r\n");
        response = response.concat("Server: httpfs/v1.0" + "\r\n");
        response = response.concat("\r\n");

        serverParameters.response = response;
    }
}