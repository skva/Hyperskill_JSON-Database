package com.gridu.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    static JsonObject db = new JsonObject();
    private static final int PORT = 34522;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock readLock = lock.readLock();
    private static final Lock writeLock = lock.writeLock();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public String set(JsonElement pos, JsonElement value) {
        JsonObject data = db;

        if (pos.isJsonArray()) {
            JsonArray path = pos.getAsJsonArray();

            for (int i = 0; i < path.size(); i++) {
                if (i == path.size() - 1) {
                    data.add(path.get(i).getAsString(), value);

                    break;
                } else if (data.has(path.get(i).getAsString())) {
                    if (!data.get(path.get(i).getAsString()).isJsonObject()) {
                        data.add(path.get(i).getAsString(), new JsonObject());
                    }
                } else {
                    data.add(path.get(i).getAsString(), new JsonObject());
                }
                data = data.getAsJsonObject(path.get(i).getAsString());
            }
        } else {
            db.add(pos.getAsString(), value);
        }
        save();
        return "{\"response\":\"OK\"}";
    }

    public String get(JsonElement pos) {
        JsonObject data = new Gson().fromJson(db, JsonObject.class);
        if (pos.isJsonArray()) {
            JsonArray path = pos.getAsJsonArray();

            if (path.size() == 1) {
                if (!data.has(pos.getAsString())) {
                    return "{\"response\": \"ERROR\", \"reason\": \"No such key\" }";
                } else {
                    save();
                    return "{\"response\": \"OK\", \"value\": " + data.get(path.get(0).getAsString()).getAsJsonObject() + "}";
                }
            } else {
                for (int i = 0; i < path.size(); i++) {
                    if (data.has(path.get(i).getAsString())) {
                        if (!data.get(path.get(i).getAsString()).isJsonObject()) {
                            if (i == path.size() - 1) {
                                return "{\"response\": \"OK\", \"value\": \"" + data.get(path.get(i).getAsString()).getAsString() + "\" }";
                            } else {
                                return "{\"response\": \"ERROR\", \"reason\": \"No such key\" }";
                            }
                        } else {
                            data = data.get(path.get(i).getAsString()).getAsJsonObject();
                        }
                    } else {
                        return "{\"response\": \"ERROR\", \"reason\": \"No such key\" }";
                    }
                }
            }
        } else {
            if (!data.has(pos.getAsString())) {
                return "{\"response\": \"ERROR\", \"reason\": \"No such key\" }";
            } else {
                return "{\"response\": \"OK\", \"value\": \"" + data.get(pos.getAsString()) + "\" }";
            }
        }
        return "{\"response\": \"ERROR\", \"reason\": \"No such key\" }";
    }

    String delete(JsonElement pos) {
        JsonObject data = db;
        if (pos.isJsonArray()) {
            JsonArray path = pos.getAsJsonArray();
            for (int i = 0; i < path.size(); i++) {
                if (!data.has(path.get(i).getAsString())) {
                    return "{\"response\": \"ERROR\", \"reason\": \"No such key\" }";
                } else if (i == path.size() - 1) {
                    if (data.has(path.get(i).getAsString())) {
                        data.remove(path.get(i).getAsString());
                        break;
                    }
                } else if (!data.get(path.get(i).getAsString()).isJsonObject()) {
                    return "{\"response\": \"ERROR\", \"reason\": \"No such key\" }";
                }
                data = data.getAsJsonObject(path.get(i).getAsString());
            }
        } else {
            if (!data.has(pos.getAsString())) {
                return "{\"response\": \"ERROR\", \"reason\": \"No such key\" }";
            } else {
                db.remove(pos.getAsString());
            }
        }
        save();
        return "{\"response\":\"OK\"}";
    }

    public void save() {
        writeLock.lock();
        try {
            FileWriter writer = new FileWriter("/Users/ebelousov/IdeaProjects/JSON Database/JSON Database/task/src/server/data/db.json");
            writer.write(db.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeLock.unlock();
    }

    public static void main(String[] args) throws IOException {
        Main main = new Main();

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server started!");

            while(true) {
                try (
                        Socket socket = server.accept(); // accepting a new client
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream())
                ) {


                    JsonObject requestJsonObject = new Gson().fromJson(input.readUTF(), JsonObject.class);

                    executor.submit(() -> {
                            }
                    );

                    switch (requestJsonObject.get("type").getAsString()) {
                        case "set" -> {
                            output.writeUTF(main.set(requestJsonObject.get("key"), requestJsonObject.get("value")));
                        }
                        case "get" -> {
                            output.writeUTF(main.get(requestJsonObject.get("key")));
                        }
                        case "delete" -> {
                            output.writeUTF(main.delete(requestJsonObject.get("key")));
                        }
                        case "exit" -> {
                            output.writeUTF("OK");
                            System.exit(0);
                        }
                        default ->
                        {
                            System.out.println("ERROR");
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}