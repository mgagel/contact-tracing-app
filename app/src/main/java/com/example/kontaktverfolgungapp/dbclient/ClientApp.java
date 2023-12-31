package com.example.kontaktverfolgungapp.dbclient;

import java.io.IOException;
import java.util.ArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ClientApp implements Communication {

    final static String serverIP = "192.168.178.94";    //private ip of server
    final static int serverPORT = 49500;
    final static int clientPORT = 49501;

    //call this function once before using other functions of the API
    public static void initServerConnection() {
        Thread myThread = new Thread(new Client.ReceiveMsgThread(serverPORT));
        myThread.start();
    }

    /* Example:
     * double timeframe = scanQR(1, 2, "2021-12-22 13:00:00");
     */
    public static double scanQR(int UID, int PID, String DateTime) {
        String msg = "scanQR;" + UID + ";" + PID + ";" + DateTime;
        try {
            Client.sendMessage(msg, serverIP, clientPORT);
        } catch (IOException e) {
            return 0.0;
        }
        msg = Client.receiveMessage();
        return Double.parseDouble(msg);
    }

    public static int newUser(String NewUser) {
        String msg = "newUser;" + NewUser;
        try {
            Client.sendMessage(msg, serverIP, clientPORT);
        } catch (IOException e) {
            return 0;
        }
        msg = Client.receiveMessage();
        return Integer.parseInt(msg);
    }

    public static void setName(int UID, String NewName) {
        String msg = "setName;" + UID + ";" + NewName;
        try {
            Client.sendMessage(msg, serverIP, clientPORT);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }

    public static ArrayList<Visit> loadVisits(int UID) {
        String msg = "loadVisits;" + UID;
        ArrayList<Visit> visits = new ArrayList<Visit>();
        try {
            Client.sendMessage(msg, serverIP, clientPORT);
        } catch (IOException e) {
            return visits;
        }
        msg = Client.receiveMessage();

        if (msg == null) {
            return visits;
        }

        String[] visitObjects = msg.split("%");

        for(int i=0; i<visitObjects.length; i++) {
            String[] visitProps = visitObjects[i].split(";");

            int VID = Integer.parseInt(visitProps[0]);
            int PID = Integer.parseInt(visitProps[1]);
            String PlaceName = visitProps[2];
            String DateTime = visitProps[3];
            double Timeframe = Double.parseDouble(visitProps[4]);

            Visit newVisit = new Visit(VID, PID, PlaceName, DateTime, Timeframe);
            visits.add(newVisit);
        }

        return visits;
    }

    public static String[] loadMetPeople(int PID, String DateTime) {
        String msg = "loadMetPeople;" + PID + ";" + DateTime;

        try {
            Client.sendMessage(msg, serverIP, clientPORT);
        } catch (IOException e) {
            return new String[0];
        }
        msg = Client.receiveMessage();

        String[] metPeople = msg.split(";");

        return metPeople;
    }
}
