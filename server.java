import java.net.*;
import java.io.*;
import java.util.*;
import java.io.File;

public class server {

  private static ServerSocket serverSocket = null;
  private static Socket clientSocket = null;
  private static final ArrayList<clientThread> threads = new ArrayList<clientThread>();
  public static void main(String args[]) {

    int portNumber = 2222;
    if (args.length < 1) {
      System.out
          .println("Usage: java MultiThreadChatServer <portNumber>\n"
              + "Now using port number=" + portNumber);
    } else {
      portNumber = Integer.valueOf(args[0]).intValue();                     //command line argument as port number
    }
    try {
      serverSocket = new ServerSocket(portNumber);                          //start server, listening on portnumber
    } catch (IOException e) {
      System.out.println(e);
    }
    while (true) {
      try {
        clientSocket = serverSocket.accept();                               //temporary socket which is then sent to a new thread
        clientThread x = new clientThread(clientSocket,threads);
        threads.add(x);                                                     //add new thread to data structure
        x.start();                                                          //start thread
 
      } catch (IOException e) {
        System.out.println(e);
      }
    }
  }
}


class clientThread extends Thread {

  private DataInputStream is = null;
  private PrintStream os = null;
  private Socket clientSocket = null;
  String nameOfThread;
  int found = 0;                                              //to find if the user exists
  private final ArrayList<clientThread> threads;

  public clientThread(Socket clientSocket, ArrayList<clientThread> threads) {
    
    this.clientSocket = clientSocket;                                     //setup Client socket from socket sent to constructor
    this.threads = threads;               //this.threads now starts pointing to threads. call by reference
  }
  
  public String getThreadName(){
    return nameOfThread;
  }
  public void run() {
                          
    ArrayList<clientThread> threads = this.threads;                         //data structure to store threads

    try {
      is = new DataInputStream(clientSocket.getInputStream());              //when Thread starts, setup i/o streams
      os = new PrintStream(clientSocket.getOutputStream());
      os.println("Enter your name.");
      String name = is.readLine().trim();
      this.nameOfThread = name;
      //setName(nameOfThread);
      os.println("Hello " + name                                              
          + " to our chat room.\nTo leave enter /quit in a new line");          
      System.out.println(name+" connected!");
      for (int i = 0; i < threads.size(); i++) {
        if (threads.get(i) != null && threads.get(i) != this) {                //broadcast the JOINING to everyone but itself
          (threads.get(i)).os.println(name+" has joined the chat room.");
        }
      }
      while (true) {
        String line = is.readLine(); 
        found =0;                                   //keep reading from client, if quit, then break out
        if (line.startsWith("/quit")) {
          break;
        }

        else if(line.startsWith("unicast"))                           
        {
          //unicast messageReceived nameOfThreadSendingMessageTo
          //using above format, extract message and thread info of to
          String message = line.substring(8,line.lastIndexOf(" "));
          String to = line.substring((line.lastIndexOf(" ")+1),line.length());
          //to check if the recipient is an active user
          for (int j = 0; j <threads.size(); j++) {
            if ((threads.get(j).getThreadName()).equals(to))
              {
                found = 1;
                
              }
            }
            if(found==0)
            {           
              os.println(to + " client not found");
            }
            else
            {
            //use getThreadName() method to send message to a specific thread
            for (int i = 0; i <threads.size(); i++) {
            if ((threads.get(i).getThreadName()).equals(to)) {           
              (threads.get(i)).os.println("<" + name + ">" + message);
              }
            }
            //print on server
            System.out.println(name+" sent personal message to "+to);
          }
          
        }
        else if(line.startsWith("Users"))
        {
          //display all users to the client that requested it
          for (int i = 0; i <threads.size(); i++) {
          os.println(threads.get(i).getThreadName());
        }
        //displayed on server
        System.out.println(name+" requsted to see active users");
        }

        else if(line.startsWith("blockcast"))
        {
          //blockcast message exceptThisThread
          //use above format to extract message and except info
          String message = line.substring(10,line.lastIndexOf(" "));
          String except = line.substring((line.lastIndexOf(" ")+1),line.length());
          //iterate over data structure, send it to everyone but itself and "except" thread
          for (int j = 0; j <threads.size(); j++)
          {
            if ((threads.get(j).getThreadName()).equals(except))
              found = 1;
          }

          if(found==0)
              os.println(except+" client not found");
            else
            {
              for (int i = 0; i <threads.size(); i++) {
              if((!((threads.get(i).getThreadName()).equals(except))) && (threads.get(i) !=this)) {           
                (threads.get(i)).os.println("<" + name + ">" + message); }
              }
              //displayed on the server
              System.out.println(name+" sent message to all except "+except);
            }
          }
        

      else if(line.startsWith("file unicast"))                        //(2)if starts with file, get ready to receive file
      {
        //file unicast nameOfFileWithoutQuotation receiverClient
        //use above command format to extract filename in "path" and to as follows
        String path = line.substring(13,line.lastIndexOf(" "));
        String to = line.substring((line.lastIndexOf(" ")+1),line.length());
        //display the following on the server
        System.out.println(this.getThreadName()+" sending File: "+path+" => sending it to: "+to);
        
        for (int i = 0; i <threads.size(); i++) {
          if ((threads.get(i).getThreadName()).equals(to)) { 
             //(3)send file to client, first intimate the specific client that file is incoming        
            (threads.get(i)).os.println("file");                       
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
    
            byte[] buffer = new byte[4096];
            int filesize = Integer.parseInt(dis.readLine().trim());
            
            // ClientsData is the folder storing a copy of all files being transferred. client -> server -> client
            String filename = "ClientsData\\"+path;
            FileOutputStream fos = new FileOutputStream(filename);
                         
            int read = 0;
            int totalRead = 0;
            int remaining = filesize;
             
            while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;                                                    //store file in server
            fos.write(buffer, 0, read);
            }
    
            fos.close();
            
            //forward stored file to specific client
            String forwardingFileName = filename;
            FileInputStream fis = new FileInputStream(forwardingFileName);
            DataOutputStream dos = new DataOutputStream((threads.get(i)).os); //specific client only
            PrintStream os = new PrintStream((threads.get(i)).os);
            File f1 = new File(forwardingFileName);
            os.println((int)f1.length());             //first send file length, then name of the file and then client name
            os.println(path);
            os.println(threads.get(i).getThreadName());
            byte[] buffers = new byte[8192];              //different byte array, buffer(s) not buffer as previous
            int count;
            
            while ((count = fis.read(buffers)) > 0) {       //file bytes being sent
              dos.write(buffers,0,count);            
            }
           
            fis.close();
          }
        }
      }
      
      else if(line.startsWith("file broadcast"))
      {
        String path = line.substring(15,line.length());
        System.out.println(this.getThreadName()+" sending File: "+path+" => broadcasting it");
        for (int i = 0; i <threads.size(); i++)
         {
          if((threads.get(i) !=this))                               //intimate everyone to get ready for incoming file   
            (threads.get(i)).os.println("file");   
         }      
            //Store file in server
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
    
            byte[] buffer = new byte[4096];
            int filesize = Integer.parseInt(dis.readLine().trim());
            
            String filename = "ClientsData\\"+path;     
            FileOutputStream fos = new FileOutputStream(filename);
            
            int read = 0;
            int totalRead = 0;
            int remaining = filesize;
             
            while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;                                                    
            fos.write(buffer, 0, read);
            }
    
            fos.close();

        //Send stored file to everyone 
        for (int i = 0; i <threads.size(); i++)
         {
          if((threads.get(i) !=this))
          {
            String forwardingFileName = filename;
            FileInputStream fis = new FileInputStream(forwardingFileName);
            DataOutputStream dos = new DataOutputStream((threads.get(i)).os);
            PrintStream os = new PrintStream((threads.get(i)).os);
            File f1 = new File(forwardingFileName);
            os.println((int)f1.length());
            os.println(path);
            os.println(threads.get(i).getThreadName());
            byte[] buffers = new byte[8192];              //different byte array, buffer(s) not buffer as previous
            int count;
            //System.out.println("before server's while loop");
            while ((count = fis.read(buffers)) > 0) {
              dos.write(buffers,0,count);            
            }
           //System.out.println("after client's while loop");
    
            fis.close();
          }  
          }                       
         }      

      else if(line.startsWith("file blockcast"))
      {
        String path = line.substring(15,line.lastIndexOf(" "));
        String except = line.substring((line.lastIndexOf(" ")+1),line.length());
        System.out.println(this.getThreadName()+" sending File: "+path+" => blocking it from: "+except);
        for (int i = 0; i <threads.size(); i++)
          {

            if((!((threads.get(i).getThreadName()).equals(except))) && (threads.get(i) !=this))                                                    
            (threads.get(i)).os.println("file");   
          }      
          //Store file in server
          DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
    
            byte[] buffer = new byte[4096];
            int filesize = Integer.parseInt(dis.readLine().trim());
           
            String filename = "ClientsData\\"+path;                    
            
          
              FileOutputStream fos = new FileOutputStream(filename);
             
            int read = 0;
            int totalRead = 0;
            int remaining = filesize;
             
            while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
           totalRead += read;
           remaining -= read;                                                    
            fos.write(buffer, 0, read);
            }
    
            fos.close();

        //Send stored file to everyone except specific client
        for (int i = 0; i <threads.size(); i++)
         {
          if((!((threads.get(i).getThreadName()).equals(except))) && (threads.get(i) !=this))
          {
            String forwardingFileName = filename;
            FileInputStream fis = new FileInputStream(forwardingFileName);
            DataOutputStream dos = new DataOutputStream((threads.get(i)).os);
            PrintStream os = new PrintStream((threads.get(i)).os);
            File f1 = new File(forwardingFileName);
            os.println((int)f1.length());
            os.println(path);
            os.println(threads.get(i).getThreadName());
            byte[] buffers = new byte[8192];              //different byte array, buffer(s) not buffer as previous
            int count;
           
            while ((count = fis.read(buffers)) > 0) {
              dos.write(buffers,0,count);            
            }
        
    
            fis.close();
          }                         
         } 

      }

        else if (line.startsWith("broadcast")){
        for (int i = 0; i <threads.size(); i++) {
          if (threads.get(i) != null && (threads.get(i) !=this)) {                         //if NOT QUIT, display "line" to everyone in the thread[]
          String message = line.substring(10,line.length());
            (threads.get(i)).os.println("<" + name + ">" + message);
          }
        }
        System.out.println(name+" broadcasted message to all");
      }

      else
      {
        os.println("Incorrect command. Try again.");
      }
    }
      for (int i = 0; i < threads.size(); i++) {
        if (threads.get(i) != null && threads.get(i) != this) {     //if QUIT, display the leaving message
          (threads.get(i)).os.println("<Server> : The user " + name
              + " has left the chat room.");
        }
      }
      System.out.println(name+" has left the chat room");
      os.println(" GoodBye " + name);
      for (int i = 0; i < threads.size(); i++) {         //Also, Say bye to one who's leaving
        if ((threads.get(i)) == this) {
          threads.remove(i);
        }
      }
      is.close();                                           //Close that client's socket and input output streams
      os.close();
      clientSocket.close();
    } catch (IOException e) {
    }
}
}