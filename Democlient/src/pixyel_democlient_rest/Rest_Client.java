/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pixyel_democlient_rest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import pixyel_democlient_rest.encryption.Encryption;
import pixyel_democlient_rest.xml.XML;

/**
 *
 * @author Josua Frank
 */
public class Rest_Client {

    String serverIP = "localhost";//IP-Adresse des Servers, zum testes localhost (Server und Client auf dem selben Computer), wird später "sharknoon.de" sein!
    //Der öffentliche Key des Servers
    String serverPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmG8OfhJrkN9/rXLh7auyUPcq7UxmYModYswChY8hIMgZO4m+cxOWopxOptUAYedjA4ZAKGp/P1g6n6YaXvtPQqIbi7G5oCT4vbh0zYFgI3wNCJlKtUX1gb6uCQW3rPinANcPtlZoIyegAsn/OW0FMZtc1x8PN0H1MQTlcCctXdJdotuljeYriO1lkRfb3GsotLIYjciMqIMKGQRQ2Rhj81bnxP9FybdNuVIjlS6Rfx9fzaZ2BKIdm7O7/Dzn9TcSZEOZdOSS7CHMMKr14O26g+bR2HiGWx8AbOH2zP3DMpR9/Y8GUrjO6QPqA+GorICGYWxIlrcm4iYx8740FsDaQQIDAQAB";
    //Der private Key des Clients
    String clientPrivateKey;

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        Rest_Client demoClient = new Rest_Client();
    }

    public Rest_Client() throws InterruptedException {
        login("blob");
        //**Beispiel: sendet ein xml mit dem node "echo" an den Server, der server schickt daraufhin selbiges zurück
        sendToServer(XML.createNewXML("request").addAttribute("storeId", storeId).addChild("echo"));
        //**Wenn man die App schließt oder ähnliches, einfach die disconnect Methode aufrufen
        sendToServer(XML.createNewXML("request").addAttribute("storeId", storeId).addChild("getItem").addChild("id").setContent("1"));
        Thread.sleep(1000);
        disconnect();
    }

    private void sendToServer(XML toSend) {
        sendToServer(toSend, true);
    }

    private void sendToServer(XML toSend, boolean encrypted) {
        try {
            String encryptedString = "";
            if (encrypted) {
                encryptedString = Encryption.encrypt(toSend.toString(), serverPublicKey);
            }

            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost("http://" + serverIP + ":7332/api/request/");

            // Request parameters and other properties.
            httppost.setEntity(new StringEntity(encryptedString));
            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            String received = EntityUtils.toString(response.getEntity());

            if (received != null && !received.isEmpty()) {
                String decrypted;
                //System.out.println(received);
                if (received.startsWith("<reply")) {
                    decrypted = received;
                } else {
                    decrypted = Encryption.decrypt(received, clientPrivateKey);
                }
                System.out.println(XML.openXML(decrypted).toStringGraph());
            }

        } catch (IOException | Encryption.EncryptionException | XML.XMLException ex) {
            System.err.println("Could not send String to Server: " + ex);
        }
    }

    private void disconnect() {
        sendToServer(XML.createNewXML("request").addAttribute("storeId", storeId).addChild("disconnect"));
        System.out.println("Habe mich beim Server abgemeldet und beende mich jetzt...");
        System.exit(0);
    }

    String storeId = "";

    private void login(String storeId) {
        try {
            this.storeId = storeId;

            //Erzeuge Client Private und Public Key
            String[] keyPair = Encryption.generateKeyPair();
            //Speichere den Private Key für andere Methoden sichtbar
            clientPrivateKey = keyPair[1];
            //Erzeuge ein XML mit einem Tag namens publickey und einem tag namens storeid, siehe Spezifikation!
            XML loginXML = XML.createNewXML("request").addAttribute("storeId", storeId).addChild("login");
            loginXML.addChildren("storeId", "publicKey");
            loginXML.getFirstChild("storeId").setContent(storeId);
            loginXML.getFirstChild("publicKey").setContent(keyPair[0]);
            //Übermittle dem Server meinen Public Key
            sendToServer(loginXML);
        } catch (Encryption.EncryptionException ex) {
            Logger.getLogger(Rest_Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
