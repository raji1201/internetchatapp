import java.io.*;
import java.net.*;

public class client implements Runnable {
  private static Socket clientSocket = null;
  private static PrintStream os = null;
  private static DataInputStream is = null;
  private static BufferedReader inputLine = null;
  private static boolean closed = false;
  
  public static void main(String[] args) {
    int portNumber = 2222;
    String host = "localhost";
    if (args.length < 2) {
      System.out.println("Usage: java client <host> <portNumber>\n" 
        + "Now using host=" + host + ", port number=" + portNumber);
    } 
    else {
      host = args[0];
      portNumber = Integer.valueOf(args[1]).intValue();                    // command line argument has port no. and host
    }

    try {
      //setup sockets and streams
      clientSocket = new Socket(host, portNumber);
      inputLine = new BufferedReader(new InputStreamReader(System.in));
      os = new PrintStream(clientSocket.getOutputStream());           
      is = new DataInputStream(clientSocket.getInputStream());
    } 
    catch (UnknownHostException e) {
      System.err.println("Host not found " + host);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for the host connection "
          + host);
    }

    if (clientSocket != null && os != null && is != null) {
      try {
        //start new thread which will keep reading from server
        new Thread(new client()).start();  
        while (!closed) {
          //read from keyboard (stdin)
          String x;
          x = inputLine.readLine().trim();      //  inputLine = new BufferedReader(new InputStreamReader(System.in));
          if(x.startsWith("file"))              //(1)if user starts typing with "file", then intimate server that file is incoming 
          {
            try{
            
            os.println(x);      //send command received from user to server
            //split using space delimiter and retreive third word which has the filename
            String retval[] = x.split(" ");
            String filename = retval[2];                              


            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            FileInputStream fis = new FileInputStream(filename);
            PrintStream os = new PrintStream(clientSocket.getOutputStream());
            File f1 = new File(filename);
            os.println((int)f1.length());
            
            //send file bytes to server
            byte[] buffer = new byte[8192];
            int count;
            while ((count = fis.read(buffer)) > 0) {
              dos.write(buffer,0,count);            
            }
    
            fis.close();
            //dos.close();  
          }
          catch(Exception e)
          {
            //do nothing
          }

          }

          else{
          os.println(x);                       //else, user just wants to send message, so read it and write it to os
          } 
        }
        os.close();
        is.close();
        clientSocket.close();
      } catch (IOException e) {
        System.err.println("IOException:  " + e);
      }
    }
  }

  public void run() {
    
    String responseLine;
    try {
      while ((responseLine = is.readLine()) != null) 
      {

        if(!responseLine.startsWith("file")){                 //meaning normal text message incoming, just print on screen
        System.out.println(responseLine);    
        }
        else
        {
          System.out.println("Get ready to receive file on client side");       //(4)receive file
         
          DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            byte[] buffer = new byte[4096];
            //receive the file size sent by the server and use it 
            int filesize = Integer.parseInt(dis.readLine().trim());
            //receive the file name from the server in "path"
            String path = dis.readLine();
            //receive name of the current thread to make a directory containing all received files
            String threadName = dis.readLine();
            String filename = threadName+"\\"+path;
            //make new dir if it doesn't exist
            File theDir = new File(threadName);
            if (!theDir.exists())
            {
              try
              {
                theDir.mkdir();
              }
              catch(SecurityException se)
              {
                System.out.println("Security Exception");
              }
            }
            //open file output stream of the file
            File tempFile = new File(filename);
            FileOutputStream fos = new FileOutputStream(tempFile);
             
            int read = 0;
            int totalRead = 0;
            int remaining = filesize;
            //read bytes from server, write to fos
            while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
           totalRead += read;
           remaining -= read;                                                  
            fos.write(buffer, 0, read);
            }

            fos.close();
            System.out.println("File received : "+filename); 

            //DO AN is.read here and block it here, force it to read in this and not go to while condition


        }                                                                       
        if (responseLine.indexOf(" GoodBye") != -1)                  //keep reading from server until Bye
          break;
      }
      closed = true;
    } catch (IOException e) {
      System.err.println("IOException:  " + e);
    }
  }
}

