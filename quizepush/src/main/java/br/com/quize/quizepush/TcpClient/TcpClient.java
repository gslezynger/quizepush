package br.com.quize.quizepush.TcpClient;

import android.util.Log;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;

public class TcpClient {

    public static final String TAG = TcpClient.class.getSimpleName();
    private static final String SERVER_IP = "pushnodes-a2551d2f448250df.elb.us-east-2.amazonaws.com"; //server IP address
    private static final int SERVER_PORT = 6969;


    private Thread listeningTread = null;
    private Boolean connected = false;

    // message to send to the server
//    private String mServerMessage;
    // sends message received notifications
//    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
//    private boolean mRun = false;
    // used to send messages
    private OutputStream mBufferOut;
    private InputStream mBufferIn;
    private Socket connection;
    private long lastSeen = new Date().getTime();

    public Boolean IsConnected(){
        return connected;
    }
    private void SetServerLastSeenNow(){
        lastSeen = new Date().getTime();
    }
    private Boolean IsAlive(){
        long lastHB = new Date().getTime() - lastSeen;
        // if last seen was more than 240 seconds ( missed two heartbeats )
        return lastHB / 1000 <= 240 || lastSeen <= 0;
    }
    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public Error Init(OnMessageAvailable listener) {

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            Log.d("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket connection = new Socket();
            connection.connect(new InetSocketAddress(serverAddr, SERVER_PORT), 30000);

            connected = true;
            Error e = StartListening(connection,listener);
            if ( e != null ){
                stopClient();
                return e;
            }

        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
            stopClient();
            return new Error(e.getMessage());
        }
        return null;
    }

    private Error StartListening(final Socket conn, final OnMessageAvailable listener){
        if (listeningTread == null) {
            try {
                //output buffer from connection
                mBufferOut = conn.getOutputStream();

                //input buffer from connection
                mBufferIn = conn.getInputStream();
            }catch(Exception e){
                return new Error(e.getMessage());
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while(connected) {
                        try {
                            if( !IsAlive() ){
                                throw new Exception("Connection closed");
                            }
                            if (mBufferIn.available() >= 10) {
                                //sets HB to next 120 seconds
                                SetServerLastSeenNow();
                                listener.messageAvailable(mBufferIn);
                            }
                            Thread.sleep(100);
                        } catch (Exception e) {
                            stopClient();
                            listener.onConnectionClosed();
                        }
                    }
                    Log.e("TCP","Read thread has been killed");
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }
        return null;
    }

    public void SendMessage(byte[] data) throws Exception{

        if( mBufferOut == null){
            throw new Exception("No Output buffer declared");
        }

        mBufferOut.write(data);
        mBufferOut.flush();
    }


//    public void sendMessage(final String message) {
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                if (mBufferOut != null) {
//                    Log.d(TAG, "Sending: " + message);
//                    mBufferOut.println(message);
//                    mBufferOut.flush();
//                }
//            }
//        };
//        Thread thread = new Thread(runnable);
//        thread.start();
//    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        try{
            connected = false;


            if (mBufferIn != null) {
                mBufferIn.close();
            }
            if (mBufferOut != null){
                mBufferOut.close();
            }

            mBufferIn = null;
            mBufferOut = null;

            connection.close();
        }catch(Exception e){

        }
    }

//    public void run() {
//
//        mRun = true;
//
//        try {
//            //here you must put your computer's IP address.
//            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
//
//            Log.d("TCP Client", "C: Connecting...");
//
//            //create a socket to make the connection with the server
//            Socket socket = new Socket(serverAddr, SERVER_PORT);
//
//            try {
//
//                //sends the message to the server
//                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
//
//                //receives the message which the server sends back
//                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//
//                //in this while the client listens for the messages sent by the server
//                while (mRun) {
//
//                    mServerMessage = mBufferIn.readLine();
//
//                    if (mServerMessage != null && mMessageListener != null) {
//                        //call the method messageReceived from MyActivity class
//                        mMessageListener.messageReceived(mServerMessage);
//                    }
//
//                }
//
//                Log.d("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");
//
//            } catch (Exception e) {
//                Log.e("TCP", "S: Error", e);
//            } finally {
//                //the socket must be closed. It is not possible to reconnect to this socket
//                // after it is closed, which means a new socket instance has to be created.
//                socket.close();
//            }
//
//        } catch (Exception e) {
//            Log.e("TCP", "C: Error", e);
//        }
//
//    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnMessageAvailable {
        void messageAvailable(InputStream conn);
        void onConnectionClosed();
    }

}
