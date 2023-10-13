import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TCPClient {
    private static Map<String, Long> fileTransferState = new HashMap<>();

    public static void main(String[] args) {
        String serverAddress = "localhost"; // Адрес сервера
        int serverPort = 8080; // Порт сервера

        try (Socket socket = new Socket(serverAddress, serverPort);
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.print("Введите команду (echo, time, upload, download, exit): ");
                String command = scanner.nextLine();
                sendCommand(dataOutputStream, command);

                if (command.equalsIgnoreCase("exit")) {
                    break;
                } else if (command.startsWith("echo ")) {
                    String echoResponse = dataInputStream.readUTF();
                    System.out.println("Ответ сервера (echo): " + echoResponse);
                } else if (command.startsWith("time")) {
                    String timeResponse = dataInputStream.readUTF();
                    System.out.println("Ответ сервера (time): " + timeResponse);
                } else if (command.startsWith("upload ")) {
                    String filePath = command.substring(7);
                    sendFile(dataOutputStream,dataInputStream, filePath);
                } else if (command.startsWith("download ")) {
                    String filePath = command.substring(12);
                    receiveFile(dataInputStream, filePath);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendCommand(DataOutputStream dataOutputStream, String command) throws IOException {
        dataOutputStream.writeUTF(command);
    }

    private static void sendFile(DataOutputStream dataOutputStream,
                                 DataInputStream dataInputStream,
                                 String filePath) throws IOException {
        String endOfFileMarker = "###END_OF_FILE###";
        File file = new File(filePath);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192]; // Увеличьте размер буфера, если нужно
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }

            // Добавляем нулевой байт для обозначения конца файла
            dataOutputStream.write(endOfFileMarker.getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();
            System.out.println("Файл " + filePath + " успешно отправлен на сервер.");

        }
    }


    private static void receiveFile(DataInputStream dataInputStream, String filePath) throws IOException {
        String endOfFileMarker = "###END_OF_FILE###";
        byte[] endOfFileMarkerBytes = endOfFileMarker.getBytes();
        long start = System.currentTimeMillis();
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[8192]; // Увеличьте размер буфера, если нужно
            int bytesRead;
            boolean eofFound = false; // Флаг для обозначения, что найден конец файла
            if(fileTransferState.containsKey(filePath)){
                fileOutputStream.getChannel().position(fileTransferState.get(filePath));
            }
            long read = 0;
            fileTransferState.put(filePath,read);
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                read += bytesRead;
                fileTransferState.put(filePath,read);
                // Проверяем, содержит ли буфер байты конца файла
                if (containsEndOfFileMarker(buffer, bytesRead, endOfFileMarkerBytes)) {
                    int endIndex = findEndOfFileMarker(buffer, bytesRead, endOfFileMarkerBytes);
                    fileOutputStream.write(buffer, 0, endIndex); // Записываем байты до конца файла
                    eofFound = true;
                    break; // Конец файла
                }
                // Записываем данные в файл
                fileOutputStream.write(buffer, 0, bytesRead);
/*                try {
                    Thread.sleep(3000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
            }

            // Если конец файла был найден, дописываем остаток данных
            if (eofFound) {
                int remainingBytes = bytesRead - endOfFileMarkerBytes.length;
                fileOutputStream.write(buffer, bytesRead - remainingBytes, remainingBytes);
            }

            fileOutputStream.flush();
            System.out.println("Файл " + filePath + " успешно получен с клиента.");
        }
        long end = System.currentTimeMillis();
        long elapsedTime = end - start;
        long fileSize = new File(filePath).length(); // Размер полученного файла в байтах
        long bitrate = (long)(fileSize / (double) elapsedTime) * 1000; // Скорость передачи в байтах в секунду
        if(fileTransferState.get(filePath) >=fileSize)
            fileTransferState.remove(filePath);
        System.out.println("Скорость передачи файла: " + bitrate + " байт/сек");
    }

    private static boolean containsEndOfFileMarker(byte[] buffer, int bytesRead, byte[] endOfFileMarkerBytes) {
        if (bytesRead < endOfFileMarkerBytes.length) {
            return false;
        }

        for (int i = 0; i <= bytesRead - endOfFileMarkerBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < endOfFileMarkerBytes.length; j++) {
                if (buffer[i + j] != endOfFileMarkerBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    private static int findEndOfFileMarker(byte[] buffer, int bytesRead, byte[] endOfFileMarkerBytes) {
        for (int i = 0; i <= bytesRead - endOfFileMarkerBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < endOfFileMarkerBytes.length; j++) {
                if (buffer[i + j] != endOfFileMarkerBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
}
