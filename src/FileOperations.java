import model.Config;
import model.ServerParameters;

import java.io.*;
import java.nio.file.*;

public class FileOperations {
    public void processFileOperation(ServerParameters serverParameters) {
        if (Config.isVerbose)
            System.out.println("Performing File Operation");

        if (serverParameters.requestType.equalsIgnoreCase("GET")) {
            if (serverParameters.filename.equalsIgnoreCase("") || serverParameters.filename.equalsIgnoreCase("/")) {
                listFiles(serverParameters);
            } else {
                readFile(serverParameters);
            }
        } else {
            writeFile(serverParameters);
        }
    }

    public synchronized void listFiles(ServerParameters serverParameters) {
        try {
            StringBuilder sb = new StringBuilder();
            String filePath = Config.path.concat(serverParameters.filename);
            Path path = Paths.get(filePath).normalize().toAbsolutePath();
            Path resolvedPath = path.resolve(path).normalize().toAbsolutePath();
            File file = new File(String.valueOf(resolvedPath));
            String[] fileList = file.list();

            if (fileList != null) {
                for (String each : fileList) {
                    sb.append(each.concat("\n"));
                }
            }
            serverParameters.fileList = sb.toString();
            serverParameters.hasFileList = true;

            if (Config.isVerbose)
                System.out.println("Successfully Finished File Operation\n");
        } catch (Exception ex) {
            System.out.println("Something went wrong. Could not list the files.");
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public synchronized void readFile(ServerParameters serverParameters) {
        try {
            System.out.println("Readfile");
            String filePath = Config.path.concat(serverParameters.filename.concat(serverParameters.extension));
            Path path = Paths.get(filePath).normalize().toAbsolutePath();
            Path resolvedPath = path.resolve(path).normalize().toAbsolutePath();

            String data = new String(Files.readAllBytes(resolvedPath));

            serverParameters.hasData = true;
            serverParameters.data = data;

            if (Config.isVerbose)
                System.out.println("Successfully Finished File Operation\n");
        } catch (FileSystemException ex) {
            serverParameters.hasData = false;
            System.out.println("Invalid Directory");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Something went wrong. Could not read the file.");
            ex.printStackTrace();
        }
    }

    public synchronized void writeFile(ServerParameters serverParameters) {
        try {
            String filePath = Config.path.concat(serverParameters.filename);
            Path path = Paths.get(filePath).normalize().toAbsolutePath();
            Path resolvedPath = path.resolve(path).normalize().toAbsolutePath();

            BufferedWriter out = new BufferedWriter(new FileWriter(resolvedPath.toString().concat(serverParameters.extension)));
            out.write(serverParameters.payload);
            out.flush();
            out.close();

            serverParameters.postSuccess = true;

            if (Config.isVerbose)
                System.out.println("Successfully Finished File Operation\n");
        } catch (FileSystemException ex) {
            System.out.println("Invalid Directory");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Something went wrong. Could not write on to the file.");
            ex.printStackTrace();
        }
    }

    public static boolean validatePath(ServerParameters serverParameters) {
        String path = Config.path.concat(serverParameters.filename);
        Path absPath = Paths.get(Config.path).normalize().toAbsolutePath();
        Path accessPath = absPath.resolve(path).normalize().toAbsolutePath();
        Path serverPath = Paths.get(Config.path).normalize().toAbsolutePath().resolve(Config.path).normalize().toAbsolutePath();

        if (Config.isVerbose)
            System.out.println("Validating File Path");

        return (accessPath.startsWith(serverPath));
    }
}