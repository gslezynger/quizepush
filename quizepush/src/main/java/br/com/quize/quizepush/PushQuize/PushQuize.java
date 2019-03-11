package br.com.quize.quizepush.PushQuize;


import android.content.Context;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import br.com.quize.quizepush.NotificationService;
import br.com.quize.quizepush.TcpClient.TcpClient;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import java.nio.file.Files;
import java.security.SecureRandom;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Random;





public class PushQuize {



    private String serverKey;
    private String clientKey;
    private Boolean EXIT_SIGNAL = false;

    //header for request heartbeat
    private byte HB  = 1;
    //header for ack
    private byte ACK  = 2;
    //header for handshake
    private byte HS = 3;
    //header for messaging
    private byte MS  = 4;
    //header for no more messages
    private byte ES  = 5;

    //message protocol
    private byte JSON  = 1;
    private byte  PBUFF  = 2;
    private byte  RAW  = 3;
    private byte  BSLICE  = 4;

    private Header header = new Header();
    private TcpClient client;
    private Random r = new Random();
    private boolean hanshaked = false;
    private boolean ended = false;



    class Header {
        byte control;
        byte protocol;
        int id;
        int size;

        private void Init(byte[] bytes){
            control = bytes[0];
            protocol = bytes[1];

            id = (bytes[2]<<0)&0x000000ff|
                    (bytes[3]<<8)&0x0000ff00|
                    (bytes[4]<< 16)&0x00ff0000|
                    (bytes[5]<< 24)&0xff000000;

            size = (bytes[6]<<0)&0x000000ff|
                    (bytes[7]<<8)&0x0000ff00|
                    (bytes[8]<< 16)&0x00ff0000|
                    (bytes[9]<< 24)&0xff000000;
        }
        private byte[] ToBytes(){

            byte[] response = new byte[10];
            response[0] = control;
            response[1] = protocol;
            //id to byte
            response[2] = (byte)(id);
            response[3] = (byte)(id >>> 8);
            response[4] = (byte)(id >>> 16);
            response[5] = (byte)(id >>> 24);

            response[6] = (byte)(size);
            response[7] = (byte)(size >>> 8);
            response[8] = (byte)(size >>> 16);
            response[9] = (byte)(size >>> 24);

            return response;
        }
    }
    class Handshake {
        byte[] serverKey =   new byte[64];
        byte[] clientToken = new byte[64];

        private Handshake( String key, String token ){
            serverKey = key.getBytes();
            clientToken = token.getBytes();
        }
        private byte[] ToBytes() throws Exception{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write(serverKey);
            outputStream.write(clientToken);

            return outputStream.toByteArray();
        }
    }
    class Message {
        byte[] messageId =   new byte[32];
        byte[] title = new byte[128];
        byte[] text = new byte[128];
        byte[] date = new byte[16];
        byte[] serverDate = new byte[16];
        byte canceled;

        private Message( byte[] data ){
            messageId = java.util.Arrays.copyOfRange(data,0,32);
            title = java.util.Arrays.copyOfRange(data,32,160);
            text = java.util.Arrays.copyOfRange(data,160,288);
            date = java.util.Arrays.copyOfRange(data,288,304);
            serverDate = java.util.Arrays.copyOfRange(data,304,320);
            canceled = data[320];
        }
        private String GetMessageText(){
            try{
                return new String(text, "UTF-8").trim();
            }catch(Exception e){
                Log.e("[PUSH_QUIZE]","ERROR GETTING UTF-8 from string");
                return new String(text).trim();
            }
        }
        private String GetMessageTitle(){
            try{
                return new String(title, "UTF-8").trim();
            }catch(Exception e){
                return new String(title);
            }
        }

        private String GetMessageDate(){
            try{
                return new String(date, "UTF-8").trim();
            }catch(Exception e){
                return new String(date).trim();
            }
        }
        private String GetMessageServerDate(){
            try{
                return new String(serverDate, "UTF-8").trim();
            }catch(Exception e){
                return new String(serverDate).trim();
            }
        }
        private String GetMessageId(){
            try{
                return new String(messageId, "UTF-8").trim();
            }catch(Exception e){
                return new String(messageId).trim();
            }
        }

    }
    public PushQuize(){
        this.serverKey = getSKeyFromSD();
        this.clientKey = getCKeyFromSD();
    }


    public static Error Subscribe(String serverKey){
        String cKey = PushQuize.getCKeyFromSD();
        if (cKey == null) {
            cKey = randomString(64);
        }
        return PushQuize.writeKeyToSD(cKey,serverKey);
    }
    public void Exit(){
        EXIT_SIGNAL = true;
        client.stopClient();
    }


    public static Error StartService(Context maincontext){
        return ForceStartService(maincontext);
    }
    private static Error ForceStartService(Context maincontext){
        String cKey = PushQuize.getCKeyFromSD();
        String sKey = PushQuize.getSKeyFromSD();

        if(cKey == null || sKey == null ){
            return new Error("Couldn't find credentials! Have you tried Subscribe ?");
        }
        NotificationService.schedule(maincontext);

        return null;
    }

    private void StartClient(){
        client = new TcpClient();

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {

                Error err =  client.Init(new TcpClient.OnMessageAvailable() {
                    @Override
                    public void messageAvailable(InputStream conn) {
                        Log.e("TCP","message available");

                        byte[] dataheader = new byte[10];

                        try{
                            int n = conn.read(dataheader);
                            if (n != 10){
                                throw new Exception("incorrect header size");
                            }
                            header.Init(dataheader);
                            //only receives messages and acks
                            if(header.control == ACK){
                                Log.e("[PUSH_QUIZE]","ACKed com sucesso");


                                hanshaked = true;

                                return ;
                            }
                            if(header.control == MS){

                                byte[] datamessage = new byte[header.size];
                                n = conn.read(datamessage);
                                if (n != header.size){
                                    throw new Exception("incorrect message size");
                                }
                                Message message = new Message(datamessage);
                                client.SendMessage(GenerateMessageAckPacket(header.id,message.messageId));

                                Log.e("[PUSH_QUIZE]","Messaged com sucesso: " + message.GetMessageTitle().trim() + message.GetMessageText().trim());
//                                phoneservice.sendNotification( message.GetMessageText().trim());
//                                Intent resultIntent = new Intent(maincontext, MainActivity.class);
//                                phoneservice.showNotificationMessage("Teste","Mensagem","2018-10-26 22:40:00",resultIntent);
                            }
                        }catch(Exception e){
                            Log.e("TCP",e.getMessage());
                        }
                    }
                    @Override
                    public void onConnectionClosed() {
                        try{
                            Log.e("Push","Error conecting to server,restarting 1");
                            Thread.sleep(1000);
                            if(EXIT_SIGNAL){
                                return;
                            }




                            StartClient();
                        }catch(Exception e){
                        }
                    }
                });

                if( err != null ){
                    Log.e("Push","Error conecting to server,restarting");
                    try{
                        Thread.sleep(1000);
                        if(!EXIT_SIGNAL){
                            StartClient();
                        }
                        return;
                    }catch(Exception e){
                    }
                }
                err = DoHandshake();

                if( err != null ){
                    Log.e("Push","Error doing handshake: " + err.getMessage());
                    StartClient();
                    return;
                }
//                    try{
//                        Thread.sleep(1000);
//                        StartClient();
//                        return;
//                    }catch(Exception e){
//                    }
//                }

//                DoHandshake();
                DoHeartBeat();
                Log.e("Push","Connection ready, waiting for messages");
            }
        });

        thread.start();
    }
    public void DownloadNotifications(final OnNotificationAvailable callback){
        client = new TcpClient();

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {

                Error err =  client.Init(new TcpClient.OnMessageAvailable() {
                    @Override
                    public void messageAvailable(InputStream conn) {
                        Log.e("TCP","message available");

                        byte[] dataheader = new byte[10];

                        try{
                            int n = conn.read(dataheader);
                            if (n != 10){
                                throw new Exception("incorrect header size");
                            }
                            header.Init(dataheader);
                            //only receives messages and acks
                            if(header.control == ACK){
                                hanshaked = true;
//                                Log.e("[PUSH_QUIZE]","ACKed com sucesso");
                                return ;
                            }
                            if(header.control == MS){

                                byte[] datamessage = new byte[header.size];
                                n = conn.read(datamessage);
                                if (n != header.size){
                                    throw new Exception("incorrect message size");
                                }
                                Message message = new Message(datamessage);

                                if (message.canceled == 0) {
                                    callback.newNotification(message.GetMessageId(),message.GetMessageTitle(),message.GetMessageText(),message.GetMessageDate(),message.GetMessageServerDate());
                                }else {
                                    callback.cancelNotification(message.GetMessageId());
                                }

                                client.SendMessage(GenerateMessageAckPacket(header.id,message.messageId));
//                                Log.e("[PUSH_QUIZE]","Messaged com sucesso: " + message.GetMessageTitle().trim() + message.GetMessageText().trim());
                            }
                            if(header.control == ES){
                                client.stopClient();
                                if(!ended){
                                    ended = true;
                                    callback.endedNotification();
                                }

                            }
                        }catch(Exception e){
                            Log.e("[PUSH_QUIZE]",e.getMessage());
                            client.stopClient();
                            if(!ended){
                                ended = true;
                                callback.endedNotification();
                            }
                        }
                    }
                    @Override
                    public void onConnectionClosed() {
                        Log.e("[PUSH_QUIZE]","Closed Connection");
                        if(!ended){
                            ended = true;
                            callback.endedNotification();
                        }

                    }
                });

                if( err != null ){
                    Log.e("Push","Error conecting to server,restarting");
                    callback.endedNotification();
                }
                err = DoHandshake();

                if( err != null ){
                    Log.e("Push","Error doing handshake: " + err.getMessage());
                    callback.endedNotification();
                    return;
                }
                err = DoHeartBeat();
                if( err != null ){
                    Log.e("Push","Error doing handshake: " + err.getMessage());
                    callback.endedNotification();
                    return;
                }

                Log.e("Push","Connection ready, waiting for messages");
            }
        });

        thread.start();
    }









    private Error DoHandshake() {
        Log.e("[PUSH_QUIZE]","Doing handshake");
        if (client == null) {
            return new Error("Connection not started");
        }

        try{
            byte[] hspacket = GenerateHandshakePacket();


            client.SendMessage(hspacket);

            Thread.sleep(10 * 1000);
            if( !hanshaked ){
                Log.e("[PUSH_QUIZE]","FAILED handshake");

                throw new Exception("FAILED HANDSHAKE");
            }
        }catch(Exception e){
            client.stopClient();
            return new Error(e.getMessage());
        }
        return null;
    }
    private Error DoHeartBeat( ) {
        Log.e("[PUSH_QUIZE]","SENDING HEART BEAT");
        byte[] data = GenerateHeartBeatPacket();

        Log.e("[PUSH_QUIZE]","Tamanho pacote: " + data.length);
        try{
            client.SendMessage(data);
        }catch(Exception e){
            return new Error("Error doing heartbeat");
        }
        return null;
    }

    private byte[] GenerateHandshakePacket() throws Exception{

        if(serverKey == "" || clientKey == "") {
            throw new Exception("HS data incorrect");
        }
        Header h = new Header();
        h.control = HS;
        h.protocol = BSLICE;
        h.size = 0 ;
        h.id = r.nextInt( 2147483647);

        Handshake hs = new Handshake(serverKey,clientKey);
        h.size = 128;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(h.ToBytes());
        outputStream.write(hs.ToBytes());

        return outputStream.toByteArray();
    }
    private byte[] GenerateMessageAckPacket(int headerid,byte[] messageId) throws Exception{

        if(serverKey == "" || clientKey == "") {
            throw new Exception("HS data incorrect");
        }
        Header h = new Header();
        h.control = ACK;
        h.protocol = BSLICE;
        h.size = 32 ;
        h.id = headerid;


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(h.ToBytes());
        outputStream.write(messageId);

        return outputStream.toByteArray();
    }
    private byte[] GenerateHeartBeatPacket() {
        Header h = new Header();
        h.control = HB;
        h.protocol = BSLICE;
        h.size = 0 ;
        h.id = r.nextInt( 2147483647);


        return h.ToBytes();
    }


    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    static String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }
    private static Error writeKeyToSD(String cKey,String sKey){

        File root = Environment.getExternalStorageDirectory();

        //creates directory


        File file = new File (root.getAbsolutePath() + "/private","myKey.txt");
        File fileServerKey = new File (root.getAbsolutePath() + "/private","serverKey.txt");

        file.getParentFile().mkdirs();
//        Log.e("[PUSH_QUIZE]",directory.toPath().toString());

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    File directory = new File(root.getAbsolutePath() + "/private");
                    Files.createDirectory(directory.toPath());
            }
//            file.mkdirs();
            fileServerKey.createNewFile();
            file.createNewFile();

            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);

            FileOutputStream f2 = new FileOutputStream(fileServerKey);
            PrintWriter pw2 = new PrintWriter(f2);

            pw.print(cKey);
            pw2.print(sKey);

            pw2.flush();
            pw2.close();
            f2.close();

            pw.flush();
            pw.close();
            f.close();

        } catch (FileNotFoundException e) {
            return new Error(e.getMessage());
        } catch (IOException e) {
            return new Error(e.getMessage());
        }
        return null;
    }
    private static String getCKeyFromSD(){

        File root = android.os.Environment.getExternalStorageDirectory();


        File file = new File (root.getAbsolutePath() + "/private","myKey.txt");
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
//                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            return null;
        }

        return text.toString();
    }
    private static String getSKeyFromSD(){

        File root = android.os.Environment.getExternalStorageDirectory();


        File file = new File (root.getAbsolutePath() + "/private","serverKey.txt");
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
//                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            return null;
        }

        return text.toString();
    }
    public interface OnNotificationAvailable {
        void endedNotification();
        void newNotification(String id, String title, String text, String date, String serverDate);
        void cancelNotification(String id);
    }
}
