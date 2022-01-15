package com.example.kontaktverfolgungapp.dbclient;

import java.io.IOException;
import java.util.ArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ClientApp {

    final static String serverIP = "79.234.8.233";
    final static int serverPORT = 49500;
    final static int clientPORT = 49501;

    /*
    IMPORTANT:
    Client needs port forwarding on serverPort 49500 when connecting from a local network
     */

    //call this function once before using other functions of the API
    //confirm that it returns true
    public boolean initServerConnection() throws IOException {
        try {
            Client.sendMessage(getPublicIP(), serverIP, clientPORT);
            System.out.println("ip succesfully send to server");
            System.out.println("waiting for server to confirm connection");
            if (Client.receiveMessage(49500).equals("connection confirmed")) {
                return true;
            }
            return false;
        } catch (IOException ex) {
            System.out.println("IOException while connecting to server");
            return false;
        }
    }

    public static double scanQR(int UID, int PID, String DateTime) throws IOException {
        String msg = "scanQR;" + UID + ";" + PID + ";" + DateTime;
        Client.sendMessage(msg, serverIP, clientPORT);
        msg = Client.receiveMessage(serverPORT);

        return Double.parseDouble(msg);
    }

    public static int newUser(String NewUser) throws IOException {
        String msg = "newUser;" + NewUser;
        Client.sendMessage(msg, serverIP, clientPORT);
        msg = Client.receiveMessage(serverPORT);

        return Integer.parseInt(msg);
    }

    public static void setName(int UID, String NewName) throws IOException {
        String msg = "setName;" + UID + ";" + NewName;
        Client.sendMessage(msg, serverIP, clientPORT);
    }

    public static ArrayList<Visit> loadVisits(int UID) throws IOException {
        String msg = "loadVisits;" + UID;
        Client.sendMessage(msg, serverIP, clientPORT);
        msg = Client.receiveMessage(serverPORT);

        ArrayList<Visit> visits = new ArrayList<Visit>();

        if (msg.isEmpty()) {
            return visits;
        }

        String[] visitObjects = msg.split("%");

        for(int i=0; i<visitObjects.length; i++) {
            String[] visitProps = visitObjects[i].split(";");

            int PID = Integer.parseInt(visitProps[0]);
            String PlaceName = visitProps[1];
            String DateTime = visitProps[2];
            double Timeframe = Double.parseDouble(visitProps[3]);

            Visit newVisit = new Visit(PID, PlaceName, DateTime, Timeframe);
            visits.add(newVisit);
        }

        return visits;
    }

    public static String[] loadMetPeople(int PID, String DateTime) throws IOException {
        String msg = "loadMetPeople;" + PID + ";" + DateTime;
        Client.sendMessage(msg, serverIP, clientPORT);
        msg = Client.receiveMessage(serverPORT);

        String[] metPeople = msg.split(";");

        return metPeople;
    }

    private static String getPublicIP()
    {
        try {

            Document doc = Jsoup.connect("http://www.checkip.org").get();
            return doc.getElementById("yourip").select("h1").first().select("span").text();

        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
            return "error: no ip found";
        }
    }
}
