import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Scanner;

public class TCPClient {
    public static void main(String[] args) {
        try {
            Socket serverSocket = new Socket("localhost", 8085);

            // Создаем потоки для чтения и записи данных
            BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

            // Создаем поток для ввода команд с консоли
            Scanner scanner = new Scanner(System.in);

            String inputLine;
            while (true) {
                // Считываем команду с консоли
                System.out.print("Введите команду (или 'CLOSE' для завершения): ");
                inputLine = scanner.nextLine();

                if (inputLine.equalsIgnoreCase("CLOSE")) {
                    // Если команда 'CLOSE', то завершаем клиент
                    break;
                } else if (inputLine.toUpperCase(Locale.ROOT).contains("UPLOAD")) {
                    String[] commandArgs = inputLine.split(" ");
                    // Если команда 'UPLOAD', то отправляем файл на сервер
                    try {
                        File file = new File(commandArgs[1]);
                        if(!file.exists())
                            throw new FileNotExistException();
                        out.println(inputLine);
                        sendFile(serverSocket.getOutputStream(), commandArgs[1]);
                        String response = in.readLine();
                        System.out.println("Сервер ответил: " + response);
                    }
                    catch (FileNotExistException e){
                        System.out.println("Файла с таким именем не существует");
                    }
                } else if (inputLine.toUpperCase(Locale.ROOT).contains("DOWNLOAD")) {
                    // Если команда 'DOWNLOAD', то получаем файл с сервера
                    out.println(inputLine);
                    String[] commandArgs = inputLine.split(" ");
                    Path path = Paths.get(commandArgs[1]);
                    receiveFile(serverSocket.getInputStream(),
                            path.getFileName().toString());
                } else {
                    // Отправляем обычную команду на сервер
                    out.println(inputLine);

                    // Принимаем и выводим ответ от сервера
                    String response = in.readLine();
                    System.out.println("Сервер ответил: " + response);
                }
            }

            // Закрытие соединения и потоков
            in.close();
            out.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(OutputStream outputStream, String fileName) throws IOException {
        // Реализуйте отправку файла на сервер с указанным именем
        try (FileInputStream fileInputStream = new FileInputStream(fileName)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("Файл " + fileName + " успешно отправлен на сервер.");
            outputStream.write("EOF".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void receiveFile(InputStream inputStream, String fileName) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).contains("EOF")) {
                    // Если в данных найден EOF, завершаем запись в файл
                    break;
                }
                if(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).contains("не существует"))
                    throw new FileNotExistException();
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("Файл " + fileName + " успешно получен с сервера.");
        }
        catch (FileNotExistException e) {
            System.out.println("Файла с таким именем не существует");
        }
    }
}
