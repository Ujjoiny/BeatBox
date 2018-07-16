import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
public class BeatBox {
  JPanel mainPanel;
  ArrayList<JCheckBox> checkboxList;
  Sequencer sequencer;
  Sequence sequence;
  Track track;
  JFrame theFrame;
  String[] instrumentNames = { "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare",
                                "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas",
                                "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
                                "High Agogo", "Open Hi Conga" };
  int[] instruments = { 35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63 };
  public static void main(String[] args) {
    new BeatBox().buildGUI();
  }
  public void buildGUI() {
    theFrame = new JFrame("Cyber BeatBox");
    theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    BorderLayout layout = new BorderLayout();
    JPanel background = new JPanel(layout);
    background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    checkboxList = new ArrayList<JCheckBox>();
    Box buttonBox = new Box(BoxLayout.Y_AXIS);
    JButton start = new JButton("Start");
    start.addActionListener(new MyStartListener());
    buttonBox.add(start);
    JButton stop = new JButton("Stop");
    stop.addActionListener(new MyStopListener());
    buttonBox.add(stop);
    JButton upTempo = new JButton("Tempo Up");
    upTempo.addActionListener(new MyUpTempoListener());
    buttonBox.add(upTempo);
    JButton downTempo = new JButton("Tempo Down");
    downTempo.addActionListener(new MyDownTempoListener());
    buttonBox.add(downTempo);
    JButton saveState = new JButton("Save");
    saveState.addActionListener(new SaveStateListener());
    buttonBox.add(saveState);
    JButton restoreState = new JButton("Restore");
    restoreState.addActionListener(new RestoreStateListener());
    buttonBox.add(restoreState);
    Box nameBox = new Box(BoxLayout.Y_AXIS);
    for (int i = 0; i < 16; ++i) {
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
    for (int i = 0; i < 256; ++i) {
      JCheckBox c = new JCheckBox();
      c.setSelected(false);
      checkboxList.add(c);
      mainPanel.add(c);
    }
    setUpMidi();
    theFrame.setBounds(50, 50, 300, 300);
    theFrame.pack();
    theFrame.setVisible(true);
  }
  public void setUpMidi() {
    try {
      sequencer = MidiSystem.getSequencer();
      sequencer.open();
      sequence = new Sequence(Sequence.PPQ, 4);
      track = sequence.createTrack();
      sequencer.setTempoInBPM(120);
    } catch (Exception ex) { ex.printStackTrace(); }
  }
  public void buildTrackAndStart() {
    int[] trackList = null;
    sequence.deleteTrack(track);
    track = sequence.createTrack();
    for (int i = 0; i < 16; ++i) {
      trackList = new int[16];
      int key = instruments[i];
      for (int j = 0; j < 16; ++j) {
        JCheckBox jc = checkboxList.get(j + 16 * i);
        if (jc.isSelected()) {
          trackList[j] = key;
        } else {
          trackList[j] = 0;
        }
      }
      makeTracks(trackList);
      track.add(makeEvent(176, 1, 127, 0, 16));
    }
    track.add(makeEvent(192, 9, 1, 0, 15));
    try {
      sequencer.setSequence(sequence);
      sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
      sequencer.start();
      sequencer.setTempoInBPM(120);
    } catch (Exception ex) { ex.printStackTrace(); }
  }
  public class MyStartListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      buildTrackAndStart();
    }
  }
  public class MyStopListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      sequencer.stop();
    }
  }
  public class MyUpTempoListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      float tempoFactor = sequencer.getTempoFactor();
      sequencer.setTempoFactor((float) (tempoFactor * 1.03));
    }
  }
  public class MyDownTempoListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      float tempoFactor = sequencer.getTempoFactor();
      sequencer.setTempoFactor((float) (tempoFactor * .97));
    }
  }
  public class SaveStateListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      boolean[] checkBoxState = new boolean[256];
      for (int i = 0; i < 256; ++i) {
        JCheckBox check = checkboxList.get(i);
        if (check.isSelected()) {
          checkBoxState[i] = true;
        }
      }
      try {
        FileOutputStream fileStream = new FileOutputStream(new File("checkbox.ser"));
        ObjectOutputStream os = new ObjectOutputStream(fileStream);
        os.writeObject(checkBoxState);
      } catch (IOException ex) {
        System.out.println("Could not save to a file.");
        ex.printStackTrace();
      }
    }
  }
  public class RestoreStateListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      boolean[] checkBoxState = null;
      try {
        FileInputStream fileInput = new FileInputStream(new File("checkbox.ser"));
        ObjectInputStream is = new ObjectInputStream(fileInput);
        checkBoxState = (boolean[]) is.readObject();
      } catch (Exception ex) {
        System.out.println("File not found!");
        ex.printStackTrace();
      }
      for (int i = 0; i < 256; ++i) {
        if (checkBoxState[i]) {
          checkboxList.get(i).setSelected(true);
        } else {
          checkboxList.get(i).setSelected(false);
        }
      }
      sequencer.stop();
      buildTrackAndStart();
    }
  }
  public void makeTracks(int[] list) {
    for (int i = 0; i < 16; ++i) {
      int key = list[i];
      if (key != 0) {
        track.add(makeEvent(144, 9, key, 100, i));
        track.add(makeEvent(128, 9, key, 100, i + 1));
      }
    }
  }
  public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
    MidiEvent event = null;
    try {
      ShortMessage a = new ShortMessage();
      a.setMessage(comd, chan, one, two);
      event = new MidiEvent(a, tick);
    } catch (Exception ex) { ex.printStackTrace(); }
    return event;
  }
}