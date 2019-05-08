package com.example.dan87.livestreamrobot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketClient extends Thread{
    //<editor-fold defaultstate="collapsed" desc="Variables">
    private MainActivity main;
    boolean runningThread = false;
    boolean runningClient = false;
    //</editor-fold>

    SocketClient(MainActivity main) {
        this.main = main;
    }

    @Override
    public void run(){
        super.run();

        try{
            while(true) {
                if(runningClient) {
                    Socket mSocket = new Socket();
                    mSocket.connect(new InetSocketAddress("192.168.43.1", 8888), 10000);
                    BufferedOutputStream outStream = new BufferedOutputStream(mSocket.getOutputStream());
                    BufferedInputStream inStream = new BufferedInputStream(mSocket.getInputStream());

                    //Define aux vars
                    boolean sendImage, sendNext;
                    byte[] data;
                    int len;
                    byte[] lenInBytes;
                    byte[] msgAck = new byte[4];
                    int msg = 0, msgLen;
                    ByteArrayOutputStream result = new ByteArrayOutputStream();

                    big_loop:
                    do {
                        if (!main.taken) {
                            //Initialize aux vars
                            sendImage = false;
                            sendNext = false;

                            //Get pre-processed byte[] from onPreviewFrame
                            data = main.receive();
                            len = data.length;
                            lenInBytes = intToBytes(len);

                            //Do while Server Acknowledged byte[].length
                            do {
                                //Send byte[].length
                                outStream.write(lenInBytes);
                                outStream.flush();

                                //Wait Ack
                                do {
                                    msgLen = inStream.read(msgAck);
                                    result.write(msgAck, 0, msgLen);
                                } while (msgLen < 4);
                                msgAck = result.toByteArray();
                                msg = convertByteArrayToInt(msgAck);
                                if (msg == 2) {
                                    break big_loop;
                                }
                                if (msg == 1) {
                                    sendImage = true;
                                }
                                result.reset();
                            } while (!sendImage);

                            //Do while Server Acknowledged byte[]
                            do {
                                //Send byte[]
                                outStream.write(data);
                                outStream.flush();

                                //Wait Ack
                                do {
                                    msgLen = inStream.read(msgAck);
                                    result.write(msgAck, 0, msgLen);
                                } while (msgLen < 4);
                                msgAck = result.toByteArray();
                                msg = convertByteArrayToInt(msgAck);
                                if (msg == 1) {
                                    sendNext = true;
                                }
                                result.reset();
                            } while (!sendNext);
                        }
                    } while (runningThread);

                    if(!runningThread){
                        //message server that client is closing
                        byte[] closingMessage = intToBytes(2);
                        outStream.write(closingMessage);
                        outStream.flush();
                    }else{
                        if (msg == 2) {
                            //Stop SocketClient communication
                            runningThread = false;
                            runningClient = false;

                            main.showToast("Server closed connection");
                        }
                    }

                    //closing streams & socket
                    outStream.close();
                    inStream.close();
                    mSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] intToBytes(final int data) {
        return new byte[] {
                (byte)((data >> 24) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data) & 0xff),
        };
    }

    private static int convertByteArrayToInt(byte[] data) {
        if (data == null || data.length != 4) return 0x0;
        // ----------
        return (
                (0xff & data[0]) << 24  |
                        (0xff & data[1]) << 16  |
                        (0xff & data[2]) << 8   |
                        (0xff & data[3])
        );
    }
}