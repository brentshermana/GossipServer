
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Stack;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Imap {
    private static int TAGCOUNTER = 0;
    private static String TAGCHAR = "A";
    private static int EMAILCOUNTER = 1;

    private String server;
    private int port;
    private String username;
    private String password;
    private boolean deleteAfterDownloading;
    private boolean downloadAllFolders;
    private ArrayList<String> folders;

    public Imap(String server, int port, String username, String password, boolean deleteAfterDownloading, boolean downloadAllFolders, ArrayList<String> folders) {
        this.server = server;
        this.username = username;
        this.password = password;
        this.deleteAfterDownloading = deleteAfterDownloading;
        this.downloadAllFolders = downloadAllFolders;
        this.folders = folders;
        this.port = port;
    }

    public void run() {
        try {
            //testing check to make sure I don't delete my entire email contents
            /*
        	while (deleteAfterDownloading) {
                System.out.print("Delete flag is set, stop now.");
                return;
            }
            */

            //create socket
            SSLSocketFactory sockFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sock = null;
            sock = (SSLSocket) sockFactory.createSocket(server, port);


            //create writer/reader
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            //login
            //create and send message
            String login = TAGCHAR + TAGCOUNTER++ + " LOGIN \"" + username + "\" \"" + password + "\"\r\n";
            out.write(login);
            out.flush();

            //receive response from server
            if (receiveLoginResponse(in, TAGCOUNTER - 1)) {
                System.out.println("Login successful");
            } else {
                System.out.println("Failed to login successfuly with given username and password"
                        + " to the given server and port, exiting.");
                return;
            }
            //login completed; should probably be extracted into its own method

            //download messages
            if (downloadAllFolders) {
                //download messages from all folders
                downloadAllFolders(out, in, deleteAfterDownloading);
            } else {
                //download messages only from specified folders
                downloadSpecificFolders(out, in, folders, deleteAfterDownloading);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main (final String[] args) throws UnknownHostException, IOException {
        //currently assuming the parameters described on the assignment description
        //are parsed by the shell script that is called to start the program
        //and are passed in to here, the actual program, as command line arguments
        //TODO: write .sh script that passes in args correctly
        //assuming order of command line arguments to be:
        //server ; port ; username ; password ; boolean for "-d" flag ;
        //boolean for "-a" flag ; folders specified with the "-f" flag, possibly multiple arguments,
        //extending to the end of the args array

        //get arguments from command line
        /*
        String server = args[0];
        int port = Integer.parseInt(args[1]);
        String username = args[2];
        String password = args[3];
        boolean deleteAfterDownloading = args[4].equals("true") ? true : false;
        boolean downloadAllFolders = args[5].equals("true") ? true : false;
        ArrayList<String> folders = new ArrayList<>();
        if (!downloadAllFolders) {
            for (int i = 6; i < args.length; i++) {
                folders.add(args[i]);
            }
        }
        
        //testing check to make sure I don't delete my entire email contents
        while (deleteAfterDownloading) {
            System.out.print("Delete flag is set, stop now.");
            return;
        }

        //create socket
        SSLSocketFactory sockFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sock = (SSLSocket) sockFactory.createSocket(server, port);

        //create writer/reader
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
        BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        //login
        //create and send message
        String login = TAGCHAR + TAGCOUNTER++ + " LOGIN \"" + username + "\" \"" + password + "\"\r\n";
        out.write(login);
        out.flush();

        //receive response from server
        if (receiveLoginResponse(in, TAGCOUNTER - 1)) {
            System.out.println("Login successful");
        } else {
            System.out.println("Failed to login successfuly with given username and password"
                    + " to the given server and port, exiting.");
            return;
        }
        //login completed; should probably be extracted into its own method

        //download messages
        if (downloadAllFolders) {
            //download messages from all folders
            downloadAllFolders(out, in, deleteAfterDownloading);
        } else {
            //download messages only from specified folders
            downloadSpecificFolders(out, in, folders, deleteAfterDownloading);
        }
        */
    }
        

    
    private void downloadSpecificFolders (BufferedWriter out, BufferedReader in,
            ArrayList<String> folders, boolean delete) throws IOException {
        //check to make sure that each mailbox/folder requested exists on the email
        //send the list of existing mailboxes/folders to downloadFromGivenMailboxes
        //also find the exact mailbox path to name so that it can be fed directly to
        //SELECT and work
        ArrayList<String> properMailboxNames = new ArrayList<>();
        for (int i = 0; i < folders.size(); i++) {
            //first test to see if the mailbox is in root directory of email
            String rootList = TAGCHAR + TAGCOUNTER++ + " LIST \"\" \"" + folders.get(i) + "\"\r\n";
            out.write(rootList);
            out.flush();
            //receive response from server
            ArrayList<String> rootListResponse = receiveResponse(in, TAGCOUNTER - 1);
            //check it
            if (rootListResponse.get(rootListResponse.size() - 1).
                    substring((Integer.toString(TAGCOUNTER - 1).length() + 2),
                            (Integer.toString(TAGCOUNTER - 1).length() + 4)).equals("OK")) {
                //command processed properly
                //check to see if the untagged response saying that the folder exists
                //in root directory of email is there
                boolean inRoot = false;
                for (int j = 0; j < rootListResponse.size() - 1; j++) {
                    String line = rootListResponse.get(j);
                    if (line.toLowerCase().contains("\"" + folders.get(i).toLowerCase() + "\"")) {
                        properMailboxNames.add(folders.get(i));
                        inRoot = true;
                        break;
                    }
                }
                //if inRoot is false, the mailbox might still be on the email, just
                //not in the root of the email, so check everywhere else
                if (!inRoot) {
                    String subdirList = TAGCHAR + TAGCOUNTER++ + " LIST \"\" \"*/"
                            + folders.get(i) + "\"\r\n";
                    out.write(subdirList);
                    out.flush();
                    
                    ArrayList<String> subdirListResponse = receiveResponse(in, TAGCOUNTER - 1);
                    
                    if (subdirListResponse.get(subdirListResponse.size() - 1).
                            substring((Integer.toString(TAGCOUNTER - 1).length() + 2),
                                    (Integer.toString(TAGCOUNTER - 1).length() + 4)).equals("OK")) {
                        //command processed successfully, check untagged responses, if any
                        boolean found = false;
                        for (int k = 0; k < subdirListResponse.size() - 1; k++) {
                            String line = subdirListResponse.get(k);
                            if (line.toLowerCase().contains("/" + folders.get(i).toLowerCase() + "\"")) {
                                //remove quote at the end of the line
                                line = line.substring(0, line.length() - 1);
                                //now that end quote is removed, find the last quote in the line
                                //that new last quote of the line begins the full mailbox name
                                //component that is needed
                                properMailboxNames.add(line.substring(line.lastIndexOf('"') + 1));
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            //mailbox with given folder name was not found on the email
                            System.out.printf("Could not find a folder/mailbox with the name"
                                    + " \"%s\" on this email, discarding folder/mailbox \"%s\".\n"
                                    , folders.get(i), folders.get(i));
                        }
                    } else {
                        System.out.printf("Failed to properly check if folder \"%s\" exists"
                                + " in a subdirectory or not, exiting.\n", folders.get(i));
                        return;
                    }
                }
            } else {
                System.out.printf("Failed to properly check if folder \"%s\" exists in root"
                        + " directory or not, exiting.\n", folders.get(i));
                return;
            } //end if
        } //end loop
        downloadFromGivenMailboxes(properMailboxNames, out, in, delete);
    }
    
    private void downloadAllFolders (BufferedWriter out, BufferedReader in,
            boolean delete) throws IOException {
        int expectedCounter = TAGCOUNTER;
        //create and send list message
        String listAllFolders = TAGCHAR + TAGCOUNTER++ + " LIST \"\" *\r\n";
        out.write(listAllFolders);
        out.flush();
        
        //receive response of mailbox listing
        ArrayList<String> mailboxInfoLines = receiveResponse(in, expectedCounter);
        
        
        //check to make sure the command completed successfully
        if (!mailboxInfoLines.get(mailboxInfoLines.size() - 1).
                substring((Integer.toString(expectedCounter).length() + 2),
                        (Integer.toString(expectedCounter).length() + 4)).equals("OK")) {
            System.out.println("Failed to properly access mailboxes on the mail server, exiting.");
            return;
        }
        //get list of all mailbox names from list of response lines
        ArrayList<String> mailboxNames = new ArrayList<>();
        for (int i= 0; i < mailboxInfoLines.size() - 1; i++) {
            String curLine = mailboxInfoLines.get(i);
            curLine = curLine.substring(7); //remove the "* LIST " at the beginning
            int endOfFlagsIndex = curLine.indexOf(')');
            String flags = curLine.substring(0, endOfFlagsIndex);
            if (!flags.contains("Noselect")) {
                //grabs the rest of the line, after skipping the flags and the
                //hierarchy delimiter
                String name = curLine.substring(endOfFlagsIndex + 6);
                if (name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"') {
                    name = name.substring(1, name.length() - 1);
                }
                mailboxNames.add(name);
            }
        }
        //gives list of mailbox names to a different method to actually download them
        downloadFromGivenMailboxes(mailboxNames, out, in, delete);

    }
    
    private void downloadFromGivenMailboxes (ArrayList<String> mailboxes,
            BufferedWriter out, BufferedReader in, boolean delete) throws IOException {
        String startDir = System.getProperty("user.dir");
        for (int i = 0; i < mailboxes.size(); i++) {
            //reset user directory
            System.setProperty("user.dir", startDir);
            //System.out.println("Mailbox of i=" + i + ":" + mailboxes.get(i) + ":");
            File curMailbox = new File(mailboxes.get(i));
            curMailbox.mkdirs();
            //send select command, then
            //receive select response and parse to find number of messages in mailbox
            
            String select = TAGCHAR + TAGCOUNTER++ + " SELECT \"" + mailboxes.get(i) + "\"\r\n";
            out.write(select);
            out.flush();
            
            //receive response lines
            ArrayList<String> responseLines = receiveResponse(in, TAGCOUNTER - 1);
            //check to make sure the select command was processed correctly
            if (responseLines.get(responseLines.size() - 1).
                    substring((Integer.toString(TAGCOUNTER - 1).length() + 2),
                            (Integer.toString(TAGCOUNTER - 1).length() + 4)).equals("OK")) {
                System.out.printf("Inbox \"%s\" selected properly...\n", mailboxes.get(i));
            } else {
                System.out.printf("Failed to select inbox \"%s\" properly, exiting.\n", mailboxes.get(i));
                return;
            }
            //find number of messages in this mailbox
            int numOfMessages = 0;
            for (int j = 0; j < responseLines.size() - 1; j++) {
                if (responseLines.get(j).substring(responseLines.get(j).length() - 6,
                        responseLines.get(j).length()).equals("EXISTS")) {
                    numOfMessages = Integer.parseInt(responseLines.get(j).
                            substring(2, responseLines.get(j).length() - 7));
                }
            }
            if (downloadMessages(numOfMessages, out, in, delete, mailboxes.get(i))) {
                //all messages in this folder successfully downloaded
                System.out.println("All messages in folder successfully downloaded.");
            } else {
                //not, exit
                return;
            }
        }
    }
    
    private boolean downloadMessages (int numOfMessages, BufferedWriter out,
            BufferedReader in, boolean delete, String mailbox) throws IOException {
        String startDir = System.getProperty("user.dir");
        for (int i = 1; i <= numOfMessages; i++) {
            //reset user directory
            System.setProperty("user.dir", startDir);
            //Send fetch command for message i metadata
            String fetch = TAGCHAR + TAGCOUNTER++ + " FETCH " + i + " envelope\r\n";
            out.write(fetch);
            out.flush();
            //receive metadata response
            ArrayList<String> envelopeFetchLines = receiveResponse(in, TAGCOUNTER - 1);
            //check that is was successful
            if (envelopeFetchLines.get(envelopeFetchLines.size() - 1).
                    substring((Integer.toString(TAGCOUNTER - 1).length() + 2),
                            (Integer.toString(TAGCOUNTER - 1).length() + 4)).equals("OK")) {
                //System.out.printf("Message %d metadata accessed properly...\n", i);
            } else {
                System.out.printf("Message %d metadata was not accessed properly, exiting.\n", i);
                return false;
            }
            String data = "";
            for (int j = 0; j < envelopeFetchLines.size() - 1; j++) {
                if (envelopeFetchLines.get(j).
                        substring(0, 8 + Integer.toString(i).length())
                        .equals(String.format("* %d FETCH", i))) {
                    data = envelopeFetchLines.get(j);
                    break;
                }
            }
            if (data.equals("")) {
                System.out.println("Problem finding message metadata in server response, exiting.");
                return false;
            }
            String emailFolderName = String.format("%06d", EMAILCOUNTER++);
            //start parsing the message metadata string for information;
            //need email of sender and subject of email
            //removes pre-data information
            data = data.substring(Integer.toString(TAGCOUNTER - 1).length() + 21);
            //removes data about date and places the beginning of the subject line
            //at the beginning of the string
            data = data.substring(data.indexOf('"') + 3);
            String subject = data.substring(0, data.indexOf('"'));
            //removes the subject data now that it has been recorded, and moves past the first two
            //opening parentheses of the "from" data
            data = data.substring(data.indexOf('(') + 2);
            //removes all "from" data and the opening parentheses and first quote of the "sender" data
            data = data.substring(data.indexOf(')') + 6);
            //remove the sender name, we only care about the email address
            data = data.substring(data.indexOf('"') + 2);
            if (data.charAt(0) == '"') {
                //there is a value for the SMTP source route that is not NIL, remove it
                //pass this opening quote
                data = data.substring(1);
                //pass the rest of the SMTP source route data and the ending quote, space, and
                //opening quote of the mailbox name field that follows
                data = data.substring(data.indexOf('"') + 3);
            } else {
                //value for the SMPT source route data field is NIL, skip the NIL,
                //and the space, and the opening quote of the mailbox name field that follows
                data = data.substring(5);
            }
            String senderEmail = data.substring(0, data.indexOf('"'));
            //after recoroding send email mailbox name, remove its data
            //also remove the ending quote, space, and opening quote of sender
            //email host name
            data = data.substring(data.indexOf('"') + 3);
            senderEmail = senderEmail + "@" + data.substring(0, data.indexOf('"'));
            
            //compile the parts into the new folder name for this message
            //first, replace all non-alphanumeric characters in the subject with "-"
            subject = subject.replaceAll("[^A-Za-z0-9]", "-");
            emailFolderName = emailFolderName + "_" + senderEmail + "_" + subject;
            //create folder for message and content.txt for message
            System.setProperty("user.dir", System.getProperty("user.dir") + File.separator + mailbox);
            File msgFolder = new File(System.getProperty("user.dir") + File.separator + emailFolderName);
            msgFolder.mkdir();
            System.setProperty("user.dir", msgFolder.getAbsolutePath());
            File content = new File(System.getProperty("user.dir") + File.separator + "content.txt");
            content.createNewFile();
            
            //send fetch body command to receive metadata about message content
            String fetchBody = TAGCHAR + TAGCOUNTER++ + " FETCH " + i + " bodystructure\r\n";
            out.write(fetchBody);
            out.flush();
            
            //receive response
            ArrayList<String> bodyFetchLines = receiveResponse(in, TAGCOUNTER - 1);
            //check that is was successful
            if (bodyFetchLines.get(bodyFetchLines.size() - 1).
                    substring((Integer.toString(TAGCOUNTER - 1).length() + 2),
                            (Integer.toString(TAGCOUNTER - 1).length() + 4)).equals("OK")) {
                //System.out.printf("Message %d body metadata accessed properly...\n", i);
            } else {
                System.out.printf("Message %d body metadata was not accessed properly, exiting.\n", i);
                return false;
            }
            //find specific line with pertinent data
            String bodyData = "";
            for (int j = 0; j < bodyFetchLines.size() - 1; j++) {
                if (bodyFetchLines.get(j).
                        substring(0, 8 + Integer.toString(i).length())
                        .equals(String.format("* %d FETCH", i))) {
                    bodyData = bodyFetchLines.get(j);
                    break;
                }
            }
            if (bodyData.equals("")) {
                System.out.println("Problem finding message body metadata in server response, exiting.");
                return false;
            }
            
            //store entire message contents in content.txt
            //create and send command to get full message content as text
            String bodyText = TAGCHAR + TAGCOUNTER++ + " FETCH " + i + " body[text]\r\n";
            out.write(bodyText);
            out.flush();
            //receive message contents response
            ArrayList<String> messageContents = receiveResponse(in, TAGCOUNTER - 1);
            //check that is was successful
            if (messageContents.get(messageContents.size() - 1).
                    substring((Integer.toString(TAGCOUNTER - 1).length() + 2),
                            (Integer.toString(TAGCOUNTER - 1).length() + 4)).equals("OK")) {
                System.out.printf("Message %d contents retrieved properly, downloading into file.\n", i);
            } else {
                System.out.printf("Message %d contents was not retrieved properly, exiting.\n", i);
                return false;
            }
            //remove first item in messageContents as it is a server response,
            //not part of the actual message contents, and remove the last item
            //as well because it is another server response item
            messageContents.remove(0);
            messageContents.remove(messageContents.size() - 1);

            //write the contents of messageContents to content.txt
            String masterKey = messageContents.get(0);
            Stack<String> keyStack = new Stack<>();
            String curKey = masterKey;
            boolean inMetadata = false;
            boolean takingInAttachment = false;
            String curFileName = "";
            String encodedData = "";
            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(content.getAbsolutePath()), "utf-8"));
            for (int j = 0; j < messageContents.size(); j++) {
                String line = messageContents.get(j);
                //put line in content.txt
                writer.write(line);
                writer.write("\n");
                //if we are parsing metadata, inspect it
                if (inMetadata) {
                    if (line.equals("")) {
                        //end of metadata
                        inMetadata = false;
                    } else if (line.length() >= 13 && line.substring(0, 12).equals("Content-Type")) {
                        if (line.contains(" boundary=")) {
                            String temp = line;
                            boolean foundKey = false;
                            while (!foundKey) {
                                if (temp.substring(temp.indexOf('=') - 8,
                                        temp.indexOf('=')).equals("boundary")) {
                                    //check if it is the last on in the line
                                    String newKey;
                                    if (temp.indexOf('=') == temp.lastIndexOf('=')) {
                                        //if it is, grab the remainder of the line after the '='
                                        newKey = temp.substring(temp.indexOf('=') + 1);
                                    } else {
                                        //if it is not, grab the key from just after the '=' until
                                        //the next ' '
                                        newKey = temp.substring(temp.indexOf('=') + 1,
                                                temp.indexOf(' ', temp.indexOf('=')));
                                    }
                                    foundKey = true;
                                    keyStack.push(curKey);
                                    curKey = "--" + newKey;
                                } else {
                                //cut off the non-boundary value
                                temp = temp.substring(temp.indexOf(' ', temp.indexOf('=')));
                                }
                            }
                        }
                    } else if (line.length() >= 33 && line.substring(0, 32).
                            equals("Content-Disposition: attachment;")) {
                        //content disposition line contains info about whether this part of
                        //the message is an attachment or not, and if it is, the file name
                        //if it is an attachment, get its file name
                        int fileNameStartIndex = line.indexOf(" filename=") + 10;
                        curFileName = line.substring(fileNameStartIndex);
                        //check to see if the file name is enclosed in quotes, and if so, remove
                        //the quotes
                        if (curFileName.charAt(0) == '"' &&
                                curFileName.charAt(curFileName.length() - 1) == '"') {
                            curFileName = curFileName.substring(1, curFileName.length() - 1);
                        }
                        takingInAttachment = true;
                    }
                }
                //check it for being a key, to signal content metadata to check as well
                if (line.equals(curKey)) {
                    //next lines until an empty line are content metadata to be checked
                    inMetadata = true;
                    //if you find a key, and are currently in "takingInAttachment" mode
                    //exit from it and create the attachment
                    if (takingInAttachment) {
                        takingInAttachment = false;
                        createAttachment(encodedData, curFileName);
                        encodedData = "";
                    }
                    
                }
                //check if it is a key-end signal
                if (line.equals(curKey + "--")) {
                    if (!keyStack.isEmpty()) {
                        curKey = keyStack.pop();
                    }
                    //if the keystack is empty, we are now done with the email message
                    //except for a closing parentheses and a newline, which will be
                    //downloaded fine without the key being still "correct" or used
                    
                    //if you find a key end, and are currently in "takingInAttachment" mode
                    //exit from it and create the attachment
                    if (takingInAttachment) {
                        takingInAttachment = false;
                        createAttachment(encodedData, curFileName);
                        encodedData = "";
                    }
                }
                
                //if in "takingInAttachment" mode at this point, and not in "inMetadata" mode,
                //then append the line to the encoded data as it is part of the attachment
                //encoding
                if (takingInAttachment && !inMetadata) {
                    encodedData += line;
                }
                
                
                
            }
            writer.close();
            
            
            //check if to delete message now that we are done with it
            if (delete) {
                System.out.println("Deleting message...");
                //delete flag is true, so delete this message
                //set its delete flag
                String deleteString = TAGCHAR + TAGCOUNTER++ + " STORE "
                        + i + " +FLAGS.SILENT (\\Deleted)\r\n";
                out.write(deleteString);
                out.flush();
                String response = in.readLine(); //this response will be only one line because of .SILENT
                if (!response.substring((Integer.toString(TAGCOUNTER - 1).length() + 2),
                            (Integer.toString(TAGCOUNTER - 1).length() + 4)).equals("OK")) {
                    System.out.println("Failed to set messages delete flag, continuing without deleting.");
                } else {
                    //now that its delete flag is set, send the expunge command
                    //to fully remove and delete it
                    String expunge = TAGCHAR + TAGCOUNTER++ + " EXPUNGE\r\n";
                    out.write(expunge);
                    out.flush();
                    ArrayList<String> expungeResponses = receiveResponse(in, TAGCOUNTER - 1);
                    if (expungeResponses.get(expungeResponses.size() - 1).
                            substring((Integer.toString(TAGCOUNTER - 1).length() + 2),
                                    (Integer.toString(TAGCOUNTER - 1).length() + 4)).equals("OK")) {
                        System.out.printf("Message %d deleted successfully.\n", i);
                    } else {
                        System.out.printf("Failed to properly delete message %d,"
                                + " continuing without deleting.\n", i);
                    }
                }
            }
        }
        
        return true;
    }
    
    private void createAttachment (String encoding, String fileName) throws IOException {
        try {
            File file = new File(System.getProperty("user.dir") + File.separator + fileName);
            byte[] decoded = Base64.getDecoder().decode(encoding);
            Files.write(file.toPath(), decoded);
            file.createNewFile();
        }
        catch (IllegalArgumentException e) {
            System.out.println("Failed to parse attachment " + fileName);
        }


    }
    
    private boolean receiveLoginResponse (BufferedReader in, int expectedCounter) throws IOException {
        ArrayList<String> responseLines = receiveResponse(in, expectedCounter);
        
        //unsure what to do with the extra optional information possibly received and stored in
        //responseLines, if anything needs to be done with it
        
        if (responseLines.get(responseLines.size() - 1).
                substring((Integer.toString(expectedCounter).length() + 2),
                        (Integer.toString(expectedCounter).length() + 4)).equals("OK")) {
            return true;
        } else {
            return false;
        }
    }
    
    private ArrayList<String> receiveResponse (BufferedReader in, int expectedCounter) throws IOException {

        boolean statusResponseReceived = false;
        ArrayList<String> preStatusResponseLines = new ArrayList<>();
        String statusResponseLine = "";
        while (!statusResponseReceived) {
            String line = in.readLine();
            //System.out.println("line is:" + line);
            if (line.length() >= Integer.toString(expectedCounter).length() + 1) {
                //if line length is less than 3, the line probably is an
                //empty line, and cannot possibly be the status response line
                //since line length is at least 3, it could possibly be the status response line
                //so check
                if (line.substring(0, (Integer.toString(expectedCounter).length() + 1)).
                        equals(TAGCHAR + String.valueOf(expectedCounter))) {
                    statusResponseReceived = true;
                    statusResponseLine = line;
                }
            }
            preStatusResponseLines.add(line);
            
        }
        
        return preStatusResponseLines;
    }
}
