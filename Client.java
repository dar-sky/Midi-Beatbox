import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.*;
import java.net.*;

public class Client {

    private JPanel mainPanel;
    private ArrayList<JCheckBox> checkboxList;
    private Sequencer sequencer;
    private Sequence sequence;
    private Sequence mySequence = null;
    private Track track;
    private JFrame theFrame;
    private JList<String> incomingList; // Specify the type parameter for JList
    private JTextField userMessage;
    private int nextNum;
    private Vector<String> listVector = new Vector<>(); // Use diamond operator
    private String userName;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
    private Socket socket;

    private final String[] instrumentNames = {
            "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
            "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell",
            "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"
    };

    private final int[] instruments = {
            35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63
    };

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
        if (args.length < 1) {
            System.out.println("Missing command-line argument for userName. 'Example: java Client darsky'");
            System.exit(1); // Exit with an error code
        }
        try {
            new Client().startUp(args[0]);
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void startUp(String name) {
        userName = name; // sets userName from command-line argument

        try {
            socket = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
            setUpMidi();
            buildGUI();
        } catch (Exception ex) {
            System.out.println("Failed to connect to server.");
            ex.printStackTrace();
        }
    }
    
    public class RemoteReader implements Runnable { // thread that reads data from the server, called in startUp method
        boolean[] checkboxState = null; // initialize state, name to display, and the obj (array list of checkboxes)
        String nameToShow = null;
        Object obj = null;

        public void run() {
            try {
                while ((obj = in.readObject()) != null) { 
                    System.out.println("Received object from server.");
                    System.out.println(obj.getClass()); 
                    String nameToShow = (String) obj; // set the name to show to the object's name
                    checkboxState = (boolean[]) in.readObject(); // cast boolean array of input steam's read object
                    otherSeqsMap.put(nameToShow, checkboxState); // add name and object state to the hash map
                    listVector.add(nameToShow); // add the name of object to list vector of strings
                    incomingList.setListData(listVector); // add new data to the incoming list
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    

    public void buildGUI() {
        theFrame = new JFrame("Virtual Beat Box"); // creates a window titled with cyber Client
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // sets default operation for closing
        BorderLayout layout = new BorderLayout(); // uses BorderLayout
        JPanel background = new JPanel(layout); // creates a new jpanel to hold the layouts
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // sets the borders around the layout

        checkboxList = new ArrayList<JCheckBox>(); // sets the checkbox list to a new ArrayList of JCheckBoxes
        Box buttonBox = new Box(BoxLayout.Y_AXIS); // uses a box layout for the buttons, set to vertical

        JButton start = new JButton("Start"); // creates a start button
        start.addActionListener(new MyStartListener()); // associated the start button with the startlistener event
        buttonBox.add(start); // adds the button to the buttonBox layout

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton serializeIt = new JButton("Save");
        serializeIt.addActionListener(new MySaveListener());
        buttonBox.add(serializeIt);

        JButton restore = new JButton("Load");
        restore.addActionListener(new MyReadInListener());
        buttonBox.add(restore);

        incomingList = new JList<>(listVector);
        incomingList.addListSelectionListener(new MyListSelectionListener()); 
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
        JScrollPane theList = new JScrollPane(incomingList); 
        buttonBox.add(theList); 
        incomingList.setListData(listVector); 

        userMessage = new JTextField(); 
        buttonBox.add(userMessage); 

        JButton sendMessage = new JButton("Send Message");
        sendMessage.addActionListener(new MySendMessageListener());
        buttonBox.add(sendMessage);

        Box nameBox = new Box(BoxLayout.Y_AXIS); 
        for (int i = 0; i < 16; i++) { 
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox); 
        background.add(BorderLayout.WEST, nameBox); 

        theFrame.getContentPane().add(background); 

        GridLayout grid = new GridLayout(16, 16); 
        grid.setVgap(0); 
        grid.setHgap(2);
        mainPanel = new JPanel(grid); 
        background.add(BorderLayout.CENTER, mainPanel); 

        for (int i = 0; i < 256; i++) { // iterates through each checkbox
            JCheckBox c = new JCheckBox(); // create a new checkBox item
            c.setSelected(false); // set the default selected state to false
            checkboxList.add(c); // store each object in the checkBox list
            mainPanel.add(c); // add the checkboxes to the mainPanel
        }

        theFrame.setBounds(50, 50, 300, 300); // set bounds for the frame
        theFrame.pack(); // pack the frame
        theFrame.setVisible(true); // allow the frame to be seen
    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer(); // sets up a new sequencer from the MidiSystem
            sequencer.open(); // opens the sequencer
            sequence = new Sequence(Sequence.PPQ, 4); // sets the sequencer parameters
            track = sequence.createTrack(); // creates a new track
            sequencer.setTempoInBPM(120); // sets the tempo at 120 beats per minute
        } catch (Exception e) {
            e.printStackTrace();
        } // throws exception and prints stack trace if above fails
    }

    public void buildTrackAndStart() {
        ArrayList<Integer> trackList = null; // holds the instruments for each

        sequence.deleteTrack(track); // clears the track
        track = sequence.createTrack(); // sets the track to a new one

        for (int i = 0; i < 16; i++) { // iterate through each row of the checkboxes
            trackList = new ArrayList<Integer>(); // set tracklist to an array list object

            for (int j = 0; j < 16; j++) { // iterate through the items in each row
                JCheckBox jc = (JCheckBox) checkboxList.get(j + (16 * i)); // get state for each checkbox item
                if (jc.isSelected()) { // if the checkBox is selected, set it to its corresponding instrument
                    int key = instruments[i]; // set the key equal to its corresponding instrument
                    trackList.add(key);
                } else {
                    trackList.add(null); // else set it to null (no sound)
                }
            }

            makeTracks(trackList); // make the track using the new trackList
            // track.add(makeEvent(176, 1, 127, 0, 16)); // make events for all 16 beats
        }

        track.add(makeEvent(192, 9, 1, 0, 15)); // ensures the Client reaches full 16 beats
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY); // allows the music to loop continuously
            sequencer.start(); // plays the sequence!
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class MyStartListener implements ActionListener { // set up actionlisteners for all the buttons
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * .97));
        }
    }

    public void makeTracks(ArrayList<Integer> list) { // called in buildTrackAndStart() method, takes the newlist as an argument
        Iterator<Integer> it = list.iterator();
        for (int i = 0; i < 16; i++) { // iterates through the newlist
            Integer num = (Integer) it.next(); // reads through each item on the list
            if (num != null) { // if the key is not set to 0, note on and off event are added to the track
                int numKey = num.intValue(); // get the value of that key
                track.add(makeEvent(144, 9, numKey, 100, i));
                track.add(makeEvent(128, 9, numKey, 100, i + 1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        // utility method for creating a MidiEvent
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }

    public class MySaveListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = new boolean[256]; // saves the STATE of the checkboxes
            for (int i = 0; i < 256; i++) { // walkthrough arraylist of checkboxes and add to boolean arraylist
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()) {
                    checkboxState[i] = true; // set to true if selected else false by default
                }
            }

            try {
                FileOutputStream fileStream = new FileOutputStream(new File("Checkbox.ser"));
                ObjectOutputStream os = new ObjectOutputStream(fileStream);
                os.writeObject(checkboxState); // write and serialize the boolean array
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public class MySendMessageListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i); // get checkBox states
                if (check.isSelected()) {
                    checkboxState[i] = true;
                }
            }
            try {
                out.writeObject(userName + nextNum++ + ": " + userMessage.getText()); // writes the message to the
                                                                                      // server, using the username
                out.writeObject(checkboxState); // writes the state of checkboxes for other users to load
            } catch (Exception ex) {
                System.out.println("Error. Connection to server was lost.");
            }
            userMessage.setText(""); // reset the text box after submission
        }
    }

    public class MyListSelectionListener implements ListSelectionListener { // whene a user selected a message to load
        public void valueChanged(ListSelectionEvent le) {
            if (!le.getValueIsAdjusting()) {
                String selected = (String) incomingList.getSelectedValue();
                if (selected != null) {
                    // go to the map and change the sequence
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected); // get associated beat pattern in
                                                                                      // the hash map otherSeqsMap
                    changeSequence(selectedState); // load the associated beat pattern
                    sequencer.stop();
                    buildTrackAndStart(); // start playing the new sequence
                }
            }
        }
    }

    public class MyPlayMineListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            if (mySequence != null) {
                sequence = mySequence; // restore to user's original sequence
            }
        }
    }

    public void changeSequence(boolean[] checkboxState) { // immediately loads the selected pattern from the list
                                                          // (called in MyListSelectionListener)
        for (int i = 0; i < 256; i++) { // go through each checkbox
            JCheckBox check = (JCheckBox) checkboxList.get(i); // get the state of the checkbox
            if (checkboxState[i]) { // if the checkbox is true
                check.setSelected(true); // set the checkBox object to checked
            } else {
                check.setSelected(false); // else set to unchecked
            }
        }
    }

    public class MyReadInListener implements ActionListener { // loads a saved pattern from the file directory
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = null; // initialize holder for previous checkbox states
            try {
                FileInputStream fileIn = new FileInputStream(new File("Checkbox.ser"));
                ObjectInputStream is = new ObjectInputStream(fileIn);
                checkboxState = (boolean[]) is.readObject(); // cast read object to boolean array, else it will be of
                                                             // type Object
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (int i = 0; i < 256; i++) { // go through actual checkboxes and restore their state
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (checkboxState[i]) {
                    check.setSelected(true);
                } else {
                    check.setSelected(false);
                }
            }

            sequencer.stop();
            buildTrackAndStart(); // rebuild sequence using the restored states
        }
    }
}