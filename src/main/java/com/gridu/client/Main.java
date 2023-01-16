package com.gridu.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 34522;
    public static void main(String[] args) throws IOException {
        System.out.println("Client started!");

        Args arguments = new Args();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);
        arguments.run();

        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output  = new DataOutputStream(socket.getOutputStream())
        ) {
            String msg;
            if (arguments.file == null) {
                msg = arguments.getJson();
            } else {
                msg = arguments.getFileJson();
            }
            System.out.println("Sent: " + msg);
            output.writeUTF(msg); // sending message to the server
            System.out.println("Received: " + input.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Args {
    @Parameter(names = {"--type", "-t"})
    String type;
    @Parameter(names = {"--key", "-k"})
    String key;
    @Parameter(names = {"--value", "-v"})
    String value;
    @Parameter(names = {"--file", "-in"})
    String file;
    Map<String, String> request = new LinkedHashMap<>();

    public void run() {
        System.out.println(type + " " + key + " " + value);
    }

    public String getJson() {
        request.put("type", type);
        if (key != null) {
            request.put("key", key);
        }
        if (value != null) {
            request.put("value", value);
        }

        Gson gson = new Gson();

        return gson.toJson(request);
    }

    public String getFileJson() throws IOException {
        Gson gson = new GsonBuilder().create();
        Object object;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get("/Users/ebelousov/IdeaProjects/JSON Database/JSON Database/task/src/client/data/" + file))) {
            object = gson.fromJson(reader, Object.class);
        }
        return new Gson().toJson(object);
    }
}
